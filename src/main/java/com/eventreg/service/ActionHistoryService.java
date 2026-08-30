package com.eventreg.service;

import com.eventreg.dto.response.UndoResponse;

public interface ActionHistoryService {
  UndoResponse getLastAction();

  UndoResponse undoLastAction(Long id);
}
