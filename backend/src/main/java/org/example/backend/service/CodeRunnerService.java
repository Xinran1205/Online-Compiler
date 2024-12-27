package org.example.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class CodeRunnerService {
    @Autowired
    private DockerSandboxService dockerSandboxService;

    /**
     *  根据语言分别调用沙箱里的python/java逻辑
     */
    public String runCodeSand(String code, String language) {
        switch (language.toLowerCase()) {
            case "java":
                return dockerSandboxService.runJavaCodeInNewContainer(code);
            case "python":
            default:
                return dockerSandboxService.runPythonCodeInNewContainer(code);
        }
    }

    /**
     * 根据不同语言执行代码
     */
    // 这里是不使用沙盒的情况，直接在后端执行代码
    // 这个函数暂时废弃，如果需要使用，得给后端的dockerfile加上python3和java环境
    public String runCode(String code, String language) {
        switch (language.toLowerCase()) {
            case "java":
                return runJavaCode(code);
            case "python":
            default:
                // 默认情况当成 python
                return runPythonCode(code);
        }
    }

    /**
     * 运行 Python 代码
     */
    private String runPythonCode(String code) {
        // Path tempFile：用于存储临时文件的路径。初始值设为 null，以便在 finally 块中判断是否需要删除该文件。
        Path tempFile = null;
        try {
            // Files.createTempFile：创建一个临时文件。
            // 前缀 "user_code_"：临时文件名的前缀，便于识别文件来源。
            // 后缀 ".py"：文件扩展名，表明这是一个 Python 文件。
            // 随机生成唯一文件名：方法会在前缀和后缀之间插入一串随机字符，确保每次生成的文件名唯一，避免文件名冲突。
            tempFile = Files.createTempFile("user_code_", ".py");

            // Files.write：将字节数组写入文件。
            // tempFile：目标文件路径。
            // code.getBytes(StandardCharsets.UTF_8)：将用户提交的代码字符串转换为 UTF-8 编码的字节数组，确保正确写入文件。
            Files.write(tempFile, code.getBytes(StandardCharsets.UTF_8));

            // 构建执行命令：python3 [tempFile]
            // 进程创建：操作系统通过系统调用（如 fork 和 exec 在Unix系统中）创建一个新进程，并执行指定的命令。
            // ProcessBuilder：用于创建和配置新进程的类。
            // 命令 "python3"：指定使用 Python 3 解释器执行代码。
            // 参数 tempFile.toString()：临时文件的路径，作为 Python 解释器的输入文件。
            // pb.redirectErrorStream(true)：将标准错误流 (stderr) 合并到标准输出流 (stdout)，这样可以统一读取所有输出，简化后续处理。
            // pb.start()：启动进程，开始执行命令。
            ProcessBuilder pb = new ProcessBuilder("python3", tempFile.toString());
            pb.redirectErrorStream(true); // 将 stderr 合并到 stdout
            Process process = pb.start();

            //process.getInputStream()：获取进程的标准输出流。
            //InputStreamReader：将字节流转换为字符流，指定使用 UTF-8 编码。
            //BufferedReader：缓冲字符输入流，便于按行读取输出。
            //StringBuilder outputBuilder：用于拼接多行输出，提升效率。
            //读取循环：逐行读取输出，并追加到 outputBuilder 中，每行后添加换行符 \n。
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)
            );
            StringBuilder outputBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                outputBuilder.append(line).append("\n");
            }

            //process.waitFor()：使当前线程等待进程结束，并返回进程的退出码。
            //退出码检查：
            //exitCode != 0：非零退出码通常表示程序执行过程中出现错误。
            //追加错误信息：将错误码信息追加到输出中，便于调试和反馈给用户。
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                outputBuilder.append("Process exited with error code: ").append(exitCode).append("\n");
            }

            return outputBuilder.toString().trim();

        } catch (Exception e) {
            return "Error: " + e.getMessage();
        } finally {
            // 确保临时文件被删除：无论 try 块中是否发生异常，finally 块都会执行，确保临时文件不会残留在服务器上，避免占用存储空间或潜在的安全风险。
            // Files.deleteIfExists(tempFile)：删除临时文件，如果文件不存在则不执行任何操作。
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException ignore) {}
            }
        }
    }

    /**
     * 运行 Java 代码
     * 注意：这里假设用户的代码中，含有一个 'public class Main' 且带有 main 方法。
     * 如果要支持多 class 的场景，需要根据需求自己扩展。
     */
    // java代码运行分两步：先编译 javac，再运行 java
    private String runJavaCode(String code) {
        // 注意：Java 运行分两步：先编译 javac，再运行 java
        // 为简单演示，这里把用户代码写到 Main.java 并强制使用类名 Main
        Path tempDir = null;
        try {
            // Files.createTempDirectory：创建一个临时文件夹。
            //前缀 "java_code_"：临时文件夹名的前缀，便于识别文件来源。
            //随机生成唯一文件夹名：方法会在前缀后插入一串随机字符，确保每次生成的文件夹名唯一，避免文件夹名冲突。
            tempDir = Files.createTempDirectory("java_code_");

            // 2. 在临时文件夹里创建 Main.java
            Path javaFilePath = tempDir.resolve("Main.java");
            Files.write(javaFilePath, code.getBytes(StandardCharsets.UTF_8));

            // 3. 编译：javac Main.java
            ProcessBuilder compilePb = new ProcessBuilder("javac", javaFilePath.toString());
            compilePb.redirectErrorStream(true);
            Process compileProcess = compilePb.start();
            BufferedReader compileReader = new BufferedReader(
                    new InputStreamReader(compileProcess.getInputStream(), StandardCharsets.UTF_8)
            );
            StringBuilder compileOutput = new StringBuilder();
            String line;
            while ((line = compileReader.readLine()) != null) {
                compileOutput.append(line).append("\n");
            }

            int compileExitCode = compileProcess.waitFor();
            if (compileExitCode != 0) {
                // 编译出错
                return "Compile Error:\n" + compileOutput.toString().trim();
            }

            // 4. 运行：java Main
            // 注意：要指定工作目录到临时文件夹，这样才能找到编译生成的 Main.class
            ProcessBuilder runPb = new ProcessBuilder("java", "Main");
            // runPb.directory(tempDir.toFile())：设置运行命令的工作目录为临时文件夹，
            // 这样 JVM 可以找到编译生成的 Main.class 文件。
            runPb.directory(tempDir.toFile());
            runPb.redirectErrorStream(true);
            Process runProcess = runPb.start();

            BufferedReader runReader = new BufferedReader(
                    new InputStreamReader(runProcess.getInputStream(), StandardCharsets.UTF_8)
            );
            StringBuilder runOutput = new StringBuilder();
            while ((line = runReader.readLine()) != null) {
                runOutput.append(line).append("\n");
            }

            int runExitCode = runProcess.waitFor();
            if (runExitCode != 0) {
                runOutput.append("Process exited with error code: ").append(runExitCode).append("\n");
            }

            return runOutput.toString().trim();

        } catch (Exception e) {
            return "Error: " + e.getMessage();
        } finally {
            // 清理临时文件夹（包括 .java, .class）
            if (tempDir != null) {
                try {
                    // Files.walk(tempDir)：递归遍历临时文件夹中的所有文件和子文件夹。
                    // .sorted((p1, p2) -> p2.compareTo(p1))：对路径进行排序，确保先删除文件再删除文件夹（因为文件夹必须为空才能被删除）。
                    // .forEach(path -> { ... })：对每个路径执行删除操作。
                    // Files.deleteIfExists(path)：删除指定路径的文件或文件夹，如果路径不存在则不执行任何操作。
                    // 异常处理：删除过程中可能会抛出 IOException，但这里选择忽略异常（catch (IOException ignore) {}），
                    // 因为文件删除失败通常不会影响主要功能。
                    Files.walk(tempDir)
                            .sorted((p1, p2) -> p2.compareTo(p1)) // 先删文件再删目录
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
