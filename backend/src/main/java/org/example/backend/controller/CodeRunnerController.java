package org.example.backend.controller;

import org.example.backend.service.CodeRunnerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
//@CrossOrigin(origins = "http://localhost:3000")
@CrossOrigin(origins = "*")
public class CodeRunnerController {

    @Autowired
    private CodeRunnerService codeRunnerService;

    @PostMapping("/run")
    public Map<String, String> runCode(@RequestBody Map<String, String> request) {
        // 从 request 中获取 code 和 language
        String code = request.get("code");
        String language = request.get("language");
        // 注意：如果前端没传 language，你可以默认成 "python" 或者做个简单判断

        // 运行代码并得到输出
        String output = codeRunnerService.runCode(code, language);

        // 返回结果
        Map<String, String> response = new HashMap<>();
        response.put("output", output);
        return response;
    }
}


