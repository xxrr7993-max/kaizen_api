package org.rod.kaizen_api.services.impl;

import org.rod.kaizen_api.dtos.checkin.*;
import org.rod.kaizen_api.dtos.stats.MonthlyDataDto;
import org.rod.kaizen_api.dtos.stats.StreakDto;
import org.rod.kaizen_api.dtos.stats.WeeklyDayDto;
import org.rod.kaizen_api.enums.VictoryCategory;
import org.rod.kaizen_api.exceptions.NotFoundException;
import org.rod.kaizen_api.exceptions.UnauthorizedException;
import org.rod.kaizen_api.models.*;
import org.rod.kaizen_api.repositories.*;
import org.rod.kaizen_api.services.CheckinService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CheckinServiceImpl implements CheckinService {

    private final CheckinRepository checkinRepository;
    private final VictoryCheckinRepository victoryCheckinRepository;
    private final SubtaskCheckinRepository subtaskCheckinRepository;
    private final DiscardableTaskRepository discardableTaskRepository;
    private final VictoryRepository victoryRepository;
    private final UserRepository userRepository;

    public CheckinServiceImpl(CheckinRepository checkinRepository,
                              VictoryCheckinRepository victoryCheckinRepository,
                              SubtaskCheckinRepository subtaskCheckinRepository,
                              DiscardableTaskRepository discardableTaskRepository,
                              VictoryRepository victoryRepository,
                              UserRepository userRepository) {
        this.checkinRepository = checkinRepository;
        this.victoryCheckinRepository = victoryCheckinRepository;
        this.subtaskCheckinRepository = subtaskCheckinRepository;
        this.discardableTaskRepository = discardableTaskRepository;
        this.victoryRepository = victoryRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public TodayCheckinDto getToday(UserModel user) {
        CheckinModel checkin = getOrCreateCheckin(user, LocalDate.now());
        ensureVictoryCheckins(user, checkin);
        return toTodayDto(checkin, user);
    }

    @Override
    @Transactional
    public TodayCheckinDto toggleVictory(UserModel user, UUID victoryId) {
        VictoryModel victory = victoryRepository.findByVictoryIdAndUser(victoryId, user)
                .orElseThrow(() -> new NotFoundException("Victory not found"));
        CheckinModel checkin = getOrCreateCheckin(user, LocalDate.now());
        ensureVictoryCheckins(user, checkin);
        VictoryCheckinModel vc = victoryCheckinRepository.findByCheckinAndVictory(checkin, victory)
                .orElseThrow(() -> new NotFoundException("Victory checkin not found"));
        vc.setCompleted(!vc.isCompleted());
        victoryCheckinRepository.save(vc);
        recomputeStreak(user);
        return toTodayDto(checkin, user);
    }

    @Override
    @Transactional
    public TodayCheckinDto toggleSubtask(UserModel user, UUID victoryId, int subtaskIndex) {
        VictoryModel victory = victoryRepository.findByVictoryIdAndUser(victoryId, user)
                .orElseThrow(() -> new NotFoundException("Victory not found"));
        CheckinModel checkin = getOrCreateCheckin(user, LocalDate.now());
        ensureVictoryCheckins(user, checkin);
        VictoryCheckinModel vc = victoryCheckinRepository.findByCheckinAndVictory(checkin, victory)
                .orElseThrow(() -> new NotFoundException("Victory checkin not found"));
        SubtaskCheckinModel sc = subtaskCheckinRepository
                .findByVictoryCheckinAndSubtaskIndex(vc, subtaskIndex)
                .orElseThrow(() -> new NotFoundException("Subtask not found"));
        sc.setCompleted(!sc.isCompleted());
        subtaskCheckinRepository.save(sc);
        return toTodayDto(checkin, user);
    }

    @Override
    public StreakDto getStreak(UserModel user) {
        return new StreakDto(user.getStreakCurrent(), user.getStreakRecord());
    }

    @Override
    @Transactional(readOnly = true)
    public List<WeeklyDayDto> getWeekly(UserModel user) {
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.minusDays(6);
        List<CheckinModel> checkins = checkinRepository
                .findByUserAndDateBetweenOrderByDateAsc(user, weekStart, today);
        Map<LocalDate, CheckinModel> byDate = checkins.stream()
                .collect(Collectors.toMap(CheckinModel::getDate, c -> c));

        List<WeeklyDayDto> result = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            result.add(buildWeeklyDay(date, byDate.get(date)));
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public MonthlyDataDto getMonthly(UserModel user, String month) {
        YearMonth ym = YearMonth.parse(month);
        List<CheckinModel> checkins = checkinRepository
                .findByUserAndDateBetweenOrderByDateAsc(user, ym.atDay(1), ym.atEndOfMonth());

        Map<String, Map<String, Boolean>> days = new LinkedHashMap<>();
        for (CheckinModel c : checkins) {
            days.put(c.getDate().toString(), buildCategoryMap(c));
        }
        return new MonthlyDataDto(month, days);
    }

    @Override
    @Transactional
    public TodayCheckinDto addDiscardable(UserModel user, DiscardableRecordDto dto) {
        CheckinModel checkin = getOrCreateCheckin(user, LocalDate.now());
        DiscardableTaskModel task = new DiscardableTaskModel();
        task.setCheckin(checkin);
        task.setGoal(dto.goal());
        discardableTaskRepository.save(task);
        ensureVictoryCheckins(user, checkin);
        return toTodayDto(checkin, user);
    }

    @Override
    @Transactional
    public TodayCheckinDto toggleDiscardable(UserModel user, UUID taskId) {
        DiscardableTaskModel task = discardableTaskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Discardable task not found"));
        if (!task.getCheckin().getUser().getUserId().equals(user.getUserId())) {
            throw new UnauthorizedException("Access denied");
        }
        task.setCompleted(!task.isCompleted());
        discardableTaskRepository.save(task);
        CheckinModel checkin = task.getCheckin();
        ensureVictoryCheckins(user, checkin);
        return toTodayDto(checkin, user);
    }

    @Override
    @Transactional
    public TodayCheckinDto deleteDiscardable(UserModel user, UUID taskId) {
        DiscardableTaskModel task = discardableTaskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Discardable task not found"));
        if (!task.getCheckin().getUser().getUserId().equals(user.getUserId())) {
            throw new UnauthorizedException("Access denied");
        }
        CheckinModel checkin = task.getCheckin();
        discardableTaskRepository.delete(task);
        ensureVictoryCheckins(user, checkin);
        return toTodayDto(checkin, user);
    }

    // ── private helpers ──────────────────────────────────────────────────────

    private CheckinModel getOrCreateCheckin(UserModel user, LocalDate date) {
        return checkinRepository.findByUserAndDate(user, date).orElseGet(() -> {
            CheckinModel c = new CheckinModel();
            c.setUser(user);
            c.setDate(date);
            return checkinRepository.save(c);
        });
    }

    private void ensureVictoryCheckins(UserModel user, CheckinModel checkin) {
        List<VictoryModel> victories = victoryRepository.findByUserOrderByOrderAsc(user);
        Set<UUID> existing = victoryCheckinRepository.findByCheckin(checkin).stream()
                .map(vc -> vc.getVictory().getVictoryId())
                .collect(Collectors.toSet());

        for (VictoryModel v : victories) {
            if (!existing.contains(v.getVictoryId())) {
                VictoryCheckinModel vc = new VictoryCheckinModel();
                vc.setCheckin(checkin);
                vc.setVictory(v);
                vc = victoryCheckinRepository.save(vc);
                for (int i = 0; i < v.getSubtasks().size(); i++) {
                    SubtaskCheckinModel sc = new SubtaskCheckinModel();
                    sc.setVictoryCheckin(vc);
                    sc.setSubtaskIndex(i);
                    subtaskCheckinRepository.save(sc);
                }
            }
        }
    }

    private void recomputeStreak(UserModel user) {
        List<CheckinModel> allCheckins = checkinRepository.findByUserOrderByDateDesc(user);
        int streak = 0;
        LocalDate expected = LocalDate.now();
        for (CheckinModel c : allCheckins) {
            boolean hasCompleted = c.getVictoryCheckins().stream()
                    .anyMatch(VictoryCheckinModel::isCompleted);
            if (c.getDate().equals(expected) && hasCompleted) {
                streak++;
                expected = expected.minusDays(1);
            } else if (c.getDate().isBefore(expected)) {
                break;
            }
        }
        user.setStreakCurrent(streak);
        if (streak > user.getStreakRecord()) user.setStreakRecord(streak);
        userRepository.save(user);
    }

    private TodayCheckinDto toTodayDto(CheckinModel checkin, UserModel user) {
        List<VictoryModel> victories = victoryRepository.findByUserOrderByOrderAsc(user);
        List<VictoryCheckinDto> vcDtos = new ArrayList<>();
        int completedCount = 0;

        for (VictoryModel v : victories) {
            Optional<VictoryCheckinModel> vcOpt = victoryCheckinRepository
                    .findByCheckinAndVictory(checkin, v);
            boolean completed = vcOpt.map(VictoryCheckinModel::isCompleted).orElse(false);
            if (completed) completedCount++;

            List<SubtaskCheckinDto> subtaskDtos = new ArrayList<>();
            if (vcOpt.isPresent()) {
                for (int i = 0; i < v.getSubtasks().size(); i++) {
                    final int idx = i;
                    boolean sc = subtaskCheckinRepository
                            .findByVictoryCheckinAndSubtaskIndex(vcOpt.get(), idx)
                            .map(SubtaskCheckinModel::isCompleted).orElse(false);
                    subtaskDtos.add(new SubtaskCheckinDto(idx, sc));
                }
            }
            vcDtos.add(new VictoryCheckinDto(v.getVictoryId(), v.getCategory(), completed, subtaskDtos));
        }

        List<DiscardableTaskDto> discardable = discardableTaskRepository.findByCheckin(checkin).stream()
                .map(t -> new DiscardableTaskDto(t.getTaskId(), t.getGoal(), t.isCompleted()))
                .toList();

        return new TodayCheckinDto(checkin.getDate(), vcDtos, completedCount, discardable);
    }

    private WeeklyDayDto buildWeeklyDay(LocalDate date, CheckinModel checkin) {
        Map<String, Boolean> victories = new LinkedHashMap<>();
        for (VictoryCategory cat : VictoryCategory.values()) {
            victories.put(cat.name().toLowerCase(), false);
        }
        int completedCount = 0;
        if (checkin != null) {
            for (VictoryCheckinModel vc : checkin.getVictoryCheckins()) {
                if (vc.isCompleted()) {
                    victories.put(vc.getVictory().getCategory().name().toLowerCase(), true);
                    completedCount++;
                }
            }
        }
        return new WeeklyDayDto(date.toString(), victories, completedCount);
    }

    private Map<String, Boolean> buildCategoryMap(CheckinModel checkin) {
        Map<String, Boolean> map = new LinkedHashMap<>();
        for (VictoryCategory cat : VictoryCategory.values()) {
            map.put(cat.name().toLowerCase(), false);
        }
        for (VictoryCheckinModel vc : checkin.getVictoryCheckins()) {
            if (vc.isCompleted()) {
                map.put(vc.getVictory().getCategory().name().toLowerCase(), true);
            }
        }
        return map;
    }
}
