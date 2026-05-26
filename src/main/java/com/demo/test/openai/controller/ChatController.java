package com.demo.test.openai.controller;

import com.demo.test.openai.model.AppChatResponse;
import com.demo.test.openai.model.AddDataResponse;
import com.demo.test.openai.service.ChatService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@RestController
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }


    @GetMapping("/chatWithFluentApi")
    public AppChatResponse chatWithFluentApi(@RequestHeader(name = "userId", required = true) final String userId,
            @RequestParam(name = "message", required = true) final String message) {
        return this.chatService.chatWithFluentApi(userId, message);
    }

    @PostMapping(value = "/uploadPDFData", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AddDataResponse uploadDataFromPDF(@RequestParam("file") final MultipartFile file) throws IOException {
        InputStream inputStream = file.getInputStream();

        this.chatService.addDataFromPDF(inputStream);
        return AddDataResponse.builder().responseCode("200").responseMessage("Uploaded PDF Data added successfully").build();
    }


}
