package com.demo.controller;

import com.demo.cluster.Client;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotNull;

/**
 * 任务控制器
 * @author artsing
 */
@RestController
@RequestMapping("tasks")
public class TaskController {

    private final Client client;

    @Autowired
    public TaskController(Client client) {
        this.client = client;
    }

    @PostMapping
    public int create(String data) {
        if (data == null || data.isEmpty()) {
            return 0;
        }
        Client.TaskObject task = new Client.TaskObject();

        client.submitTask(data, task);
        return 1;
    }
}
