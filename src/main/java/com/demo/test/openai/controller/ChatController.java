package com.demo.test.openai.controller;

import com.demo.test.openai.model.AppChatResponse;
import com.demo.test.openai.model.AddDataResponse;
import com.demo.test.openai.service.ChatService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

    @GetMapping("/addFullData")
    public AddDataResponse addFullData() {
        List<String> travelPolicy = List.of(
                "All business travel must be approved by the reporting manager before bookings are made.",
                "Employees should choose cost-effective travel and accommodation options aligned with company budgets.",
                "Travel expenses must be supported with valid bills and submitted within five working days after the trip.",
                "The company will reimburse approved expenses related to transportation, lodging, meals, and official business activities.",
                "Employees are expected to maintain professional behavior and comply with local laws during travel.",
                "Personal expenses, entertainment, and unauthorized upgrades will not be reimbursed by the company.",
                "Employees should prioritize safety and immediately report any emergencies or incidents to management.",
                "Company data, devices, and confidential information must be protected at all times while traveling.",
                "Any changes to approved travel plans must be communicated promptly to the manager and HR department.",
                "Failure to comply with the travel policy may result in reimbursement delays or disciplinary action."
        );
        this.chatService.addData(travelPolicy);
        return AddDataResponse.builder().responseCode("200").responseMessage("Data added successfully").build();
    }


}
