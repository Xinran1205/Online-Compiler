package org.example.backend.service;

import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.UUID;

/**
 *  每次执行用户代码都启动一个独立 Docker 容器
 *  然后通过挂载宿主机临时目录的方式，把用户脚本带进容器执行
 *  容器退出后自动 --rm 删除，最后我们也会删除宿主机的临时目录
 */
@Service
public class DockerSandboxService {

    // 这里写好镜像名称，可以放到 application.properties 里做配置
    private static final String DOCKER_IMAGE = "my-runner-image:latest";

    /**
     * 运行 Python 代码：在独立容器中执行
     */

    // 在 Docker 中，挂载卷（Volumes） 是宿主机和容器之间共享文件和目录的一种机制。
    // 通过挂载卷，容器内的应用可以访问宿主机上的文件系统，而无需关心宿主机的具体路径。
    public String runPythonCodeInNewContainer(String code) {
        Path tempDirInContainer = null;
        try {
            // 1) 在后端容器里（/mytemp）创建一个临时目录
            // /mytemp 目录：
            // 根据docker-compose.yml，宿主机的 ./tempfiles 目录被挂载到后端容器内的 /mytemp 目录。
            // 这意味着在容器内 /mytemp 目录下创建的任何文件或目录，都会实际存储在宿主机的 ./tempfiles 目录中。
            // 为什么这么做？这种做法使得代码不依赖于宿主机的具体路径，提高了代码的可移植性。
            // 无论宿主机的实际路径是什么，容器内的应用始终通过 /mytemp 进行访问。这简化了代码逻辑，避免了在不同环境下修改代码以适应不同路径的问题。
            // 只需要动配置文件即可
            Path containerMountedDir = Paths.get("/mytemp");
            // 使用 Files.createTempDirectory 在 /mytemp 下创建一个以 py_sandbox_ 开头的临时目录，例如 /mytemp/py_sandbox_12345。
            // 这个临时目录用于存放即将运行的 Python 脚本。
            tempDirInContainer = Files.createTempDirectory(containerMountedDir, "py_sandbox_");

            // 2) 写脚本到容器内 /mytemp/py_sandbox_xxx/script.py
            Path scriptFileInContainer = tempDirInContainer.resolve("script.py");
            Files.write(scriptFileInContainer, code.getBytes(StandardCharsets.UTF_8));

            // === 核心：拿到宿主机真实绝对路径，比如 /home/ubuntu/Online-Compiler/tempfiles ===
            String hostTempfilesRoot = System.getenv("HOST_TEMPFILES_ROOT");
            // getFileName() 例如 "py_sandbox_12345"
            String subDirName = tempDirInContainer.getFileName().toString();
            // 拼出宿主机对应路径
            Path hostDir = Paths.get(hostTempfilesRoot, subDirName);

            // 3) 通过 docker run -v <宿主机路径>:/workspace ...
            String containerName = "sandbox_python_" + UUID.randomUUID().toString().replaceAll("-", "");
            // 参数解释：
            // --rm：容器退出后自动删除，确保不留残余。
            // --name：指定容器名称。
            // -v /home/ubuntu/Online-Compiler/tempfiles/py_sandbox_12345:/workspace：将宿主机的临时目录挂载到容器内的 /workspace 目录。
            // my-runner-image:latest：使用预先构建好的沙箱镜像。
            // python3 /workspace/script.py：在容器内执行 Python 脚本。
            ProcessBuilder pb = new ProcessBuilder(
                    "docker", "run", "--rm",
                    "--name", containerName,
                    "-v", hostDir.toString() + ":/workspace",
                    "my-runner-image:latest",
                    "python3", "/workspace/script.py"
            );

            // 4) 拿输出
            pb.redirectErrorStream(true);
            Process process = pb.start();
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)
            );
            StringBuilder outputBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                outputBuilder.append(line).append("\n");
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                outputBuilder.append("Process exited with error code: ").append(exitCode).append("\n");
            }
            return outputBuilder.toString().trim();

        } catch (Exception e) {
            return "Error: " + e.getMessage();
        } finally {
            // 5) 清理临时文件
            if (tempDirInContainer != null) {
                try {
                    Files.walk(tempDirInContainer)
                            .sorted((p1, p2) -> p2.compareTo(p1))
                            .forEach(path -> {
                                try {
                                    Files.deleteIfExists(path);
                                } catch (IOException ignore) {}
                            });
                } catch (IOException ignore) {}
            }
        }
    }


    /**
     * 运行 Java 代码：在独立容器中执行
     * 要求用户代码里有 public class Main + main方法
     */
    public String runJavaCodeInNewContainer(String code) {
        Path tempDirInContainer = null;
        try {
            // 1) 在后端容器里（/mytemp）创建一个临时目录
            Path containerMountedDir = Paths.get("/mytemp");
            tempDirInContainer = Files.createTempDirectory(containerMountedDir, "java_sandbox_");

            // 2) 写 Main.java 到容器内 /mytemp/java_sandbox_xxx/Main.java
            Path javaFileInContainer = tempDirInContainer.resolve("Main.java");
            Files.write(javaFileInContainer, code.getBytes(StandardCharsets.UTF_8));

            // === 关键：拿到宿主机真实绝对路径，比如 /home/ubuntu/Online-Compiler/tempfiles ===
            String hostTempfilesRoot = System.getenv("HOST_TEMPFILES_ROOT");
            // 比如 tempDirInContainer = /mytemp/java_sandbox_12345
            // getFileName() => "java_sandbox_12345"
            String subDirName = tempDirInContainer.getFileName().toString();
            // 组装成宿主机对应的路径 => /home/ubuntu/Online-Compiler/tempfiles/java_sandbox_12345
            Path hostDir = Paths.get(hostTempfilesRoot, subDirName);

            // 3) 构建 docker 命令
            //    这里我们一次性执行：cd /workspace && javac Main.java && java Main
            String containerName = "sandbox_java_" + UUID.randomUUID().toString().replaceAll("-", "");
            String command = "cd /workspace && javac Main.java && java Main";

            ProcessBuilder pb = new ProcessBuilder(
                    "docker", "run", "--rm",
                    "--name", containerName,
                    "-v", hostDir.toString() + ":/workspace",
                    "my-runner-image:latest",
                    "/bin/bash", "-c", command
            );

            // 4) 启动进程并读取输出
            pb.redirectErrorStream(true);
            Process process = pb.start();
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)
            );
            StringBuilder outputBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                outputBuilder.append(line).append("\n");
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                outputBuilder.append("Process exited with error code: ").append(exitCode).append("\n");
            }
            return outputBuilder.toString().trim();

        } catch (Exception e) {
            return "Error: " + e.getMessage();
        } finally {
            // 5) 清理临时文件
            if (tempDirInContainer != null) {
                try {
                    Files.walk(tempDirInContainer)
                            .sorted((p1, p2) -> p2.compareTo(p1))
                            .forEach(path -> {
                                try {
                                    Files.deleteIfExists(path);
                                } catch (IOException ignore) {}
                            });
                } catch (IOException ignore) {}
            }
        }
    }


}
