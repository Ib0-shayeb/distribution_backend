package com.example.distribution_backernd.dto;

import com.example.distribution_backernd.model.Checklist;
import com.example.distribution_backernd.model.ChecklistItem;

import java.util.List;

public record ChecklistWithItemsDTO(
    Checklist checklist,
    List<ChecklistItem> items
) {}
