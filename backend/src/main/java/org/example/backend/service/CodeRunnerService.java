package org.example.backend.service;

import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class CodeRunnerService {

    public String runCode(String code) {
        Path tempFile = null;
        try {
            // 创建临时文件
            tempFile = Files.createTempFile("user_code_", ".py");
            // 使用 UTF-8 编码写入文件
            Files.write(tempFile, code.getBytes(StandardCharsets.UTF_8));

            // 使用本机python3执行该文件
            ProcessBuilder pb = new ProcessBuilder("python3", tempFile.toString());
            pb.redirectErrorStream(true); // 将stderr合并到stdout
            Process process = pb.start();

            // 读取执行输出
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
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
            // 清理临时文件
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (Exception ignore) {}
            }
        }
    }
}
