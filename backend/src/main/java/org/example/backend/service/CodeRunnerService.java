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
        Path tempFile = null;
        try {
            // 创建临时文件：后缀 .py
            // createTempFile 方法会在前缀和后缀之间插入一串随机生成的字符，
            // 以确保文件名的唯一性。这意味着每次调用该方法时，生成的文件名都是不同的，避免了命名冲突。
            tempFile = Files.createTempFile("user_code_", ".py");

            // UTF-8 写入用户代码
            Files.write(tempFile, code.getBytes(StandardCharsets.UTF_8));

            // 构建执行命令：python3 [tempFile]
            // 进程创建：操作系统通过系统调用（如 fork 和 exec 在Unix系统中）创建一个新进程，并执行指定的命令。
            ProcessBuilder pb = new ProcessBuilder("python3", tempFile.toString());
            pb.redirectErrorStream(true); // 将 stderr 合并到 stdout
            Process process = pb.start();

            // 读取执行输出
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
            // 运行完毕后删除临时文件
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
    private String runJavaCode(String code) {
        // 注意：Java 运行分两步：先编译 javac，再运行 java
        // 为简单演示，这里把用户代码写到 Main.java 并强制使用类名 Main
        Path tempDir = null;
        try {
            // 1. 创建临时文件夹
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
