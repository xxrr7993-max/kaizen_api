package org.rod.kaizen_api.services.impl;

import org.rod.kaizen_api.dtos.victory.VictoryGoalDto;
import org.rod.kaizen_api.dtos.victory.VictoryRecordDto;
import org.rod.kaizen_api.exceptions.NotFoundException;
import org.rod.kaizen_api.models.UserModel;
import org.rod.kaizen_api.models.VictoryModel;
import org.rod.kaizen_api.repositories.VictoryRepository;
import org.rod.kaizen_api.services.VictoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class VictoryServiceImpl implements VictoryService {

    private final VictoryRepository victoryRepository;

    public VictoryServiceImpl(VictoryRepository victoryRepository) {
        this.victoryRepository = victoryRepository;
    }

    @Override
    public List<VictoryRecordDto> findAll(UserModel user) {
        return victoryRepository.findByUserOrderByOrderAsc(user)
                .stream().map(this::toDto).toList();
    }

    @Override
    @Transactional
    public List<VictoryRecordDto> saveAll(UserModel user, List<VictoryGoalDto> goals) {
        victoryRepository.deleteByUser(user);
        List<VictoryModel> saved = new ArrayList<>();
        for (int i = 0; i < goals.size(); i++) {
            VictoryGoalDto g = goals.get(i);
            VictoryModel v = new VictoryModel();
            v.setUser(user);
            v.setCategory(g.category());
            v.setGoal(g.goal());
            v.setSubtasks(g.subtasks() != null ? g.subtasks() : new ArrayList<>());
            v.setOrder(i);
            saved.add(victoryRepository.save(v));
        }
        return saved.stream().map(this::toDto).toList();
    }

    @Override
    @Transactional
    public VictoryRecordDto create(UserModel user, VictoryGoalDto goal) {
        VictoryModel v = new VictoryModel();
        v.setUser(user);
        v.setCategory(goal.category());
        v.setGoal(goal.goal());
        v.setSubtasks(goal.subtasks() != null ? goal.subtasks() : new ArrayList<>());
        int currentMax = victoryRepository.findByUserOrderByOrderAsc(user).size();
        v.setOrder(currentMax);
        return toDto(victoryRepository.save(v));
    }

    @Override
    @Transactional
    public VictoryRecordDto update(UserModel user, UUID id, VictoryGoalDto goal) {
        VictoryModel v = victoryRepository.findByVictoryIdAndUser(id, user)
                .orElseThrow(() -> new NotFoundException("Victory not found"));
        v.setGoal(goal.goal());
        if (goal.subtasks() != null) v.setSubtasks(goal.subtasks());
        return toDto(victoryRepository.save(v));
    }

    @Override
    @Transactional
    public void delete(UserModel user, UUID id) {
        VictoryModel v = victoryRepository.findByVictoryIdAndUser(id, user)
                .orElseThrow(() -> new NotFoundException("Victory not found"));
        victoryRepository.delete(v);
    }

    @Override
    @Transactional
    public void deleteAll(UserModel user) {
        victoryRepository.deleteByUser(user);
    }

    private VictoryRecordDto toDto(VictoryModel v) {
        return new VictoryRecordDto(
                v.getVictoryId(), v.getCategory(), v.getGoal(), v.getSubtasks(), v.getOrder()
        );
    }
}
