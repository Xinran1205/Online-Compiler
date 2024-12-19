package org.example.backend.controller;

import org.example.backend.service.CodeRunnerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
//@CrossOrigin(origins = "*")
@CrossOrigin(origins = "http://localhost:5176")
public class CodeRunnerController {

    @Autowired
    private CodeRunnerService codeRunnerService;

    @PostMapping("/run")
    public Map<String, String> runCode(@RequestBody Map<String, String> request) {
        String code = request.get("code");
        String output = codeRunnerService.runCode(code);
        Map<String, String> response = new HashMap<>();
        response.put("output", output);
        return response;
    }
}

