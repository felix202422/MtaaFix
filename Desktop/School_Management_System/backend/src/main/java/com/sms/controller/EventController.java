package com.sms.controller;

import com.sms.dto.response.ApiResponse;
import com.sms.entity.Event;
import com.sms.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventRepository eventRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Event>>> getAllEvents() {
        return ResponseEntity.ok(ApiResponse.success(eventRepository.findAll()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Event>> createEvent(@RequestBody Event event) {
        return ResponseEntity.ok(ApiResponse.success("Event created", eventRepository.save(event)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteEvent(@PathVariable Long id) {
        eventRepository.deleteById(id);
        return ResponseEntity.ok(ApiResponse.success("Event deleted", null));
    }

    public EventController(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

}
