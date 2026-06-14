package org.rod.kaizen_api.services.impl;

import org.rod.kaizen_api.dtos.stats.*;
import org.rod.kaizen_api.enums.VictoryCategory;
import org.rod.kaizen_api.models.*;
import org.rod.kaizen_api.repositories.CheckinRepository;
import org.rod.kaizen_api.services.StatsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class StatsServiceImpl implements StatsService {

    private static final Map<Integer, String> PT_MONTHS = Map.ofEntries(
            Map.entry(1, "Jan"), Map.entry(2, "Fev"), Map.entry(3, "Mar"),
            Map.entry(4, "Abr"), Map.entry(5, "Mai"), Map.entry(6, "Jun"),
            Map.entry(7, "Jul"), Map.entry(8, "Ago"), Map.entry(9, "Set"),
            Map.entry(10, "Out"), Map.entry(11, "Nov"), Map.entry(12, "Dez")
    );

    private final CheckinRepository checkinRepository;

    public StatsServiceImpl(CheckinRepository checkinRepository) {
        this.checkinRepository = checkinRepository;
    }

    @Override
    public ProgressStatsDto getProgress(UserModel user, int months) {
        List<ProgressStatsDto.ProgressMonthDto> monthDtos = new ArrayList<>();
        YearMonth current = YearMonth.now();

        for (int i = months - 1; i >= 0; i--) {
            YearMonth ym = current.minusMonths(i);
            List<CheckinModel> checkins = checkinRepository
                    .findByUserAndDateBetweenOrderByDateAsc(user, ym.atDay(1), ym.atEndOfMonth());

            int totalDays = ym.lengthOfMonth();
            int fisica = 0, mental = 0, espiritual = 0, pessoal = 0;
            for (CheckinModel c : checkins) {
                for (VictoryCheckinModel vc : c.getVictoryCheckins()) {
                    if (!vc.isCompleted()) continue;
                    switch (vc.getVictory().getCategory()) {
                        case FISICA -> fisica++;
                        case MENTAL -> mental++;
                        case ESPIRITUAL -> espiritual++;
                        case PESSOAL -> pessoal++;
                    }
                }
            }
            int fp = pct(fisica, totalDays), mp = pct(mental, totalDays),
                ep = pct(espiritual, totalDays), pp = pct(pessoal, totalDays);
            int overall = (fp + mp + ep + pp) / 4;
            monthDtos.add(new ProgressStatsDto.ProgressMonthDto(
                    PT_MONTHS.get(ym.getMonthValue()), fp, mp, ep, pp, overall
            ));
        }

        // Current month completion %
        YearMonth thisMonth = YearMonth.now();
        List<CheckinModel> thisCheckins = checkinRepository
                .findByUserAndDateBetweenOrderByDateAsc(user, thisMonth.atDay(1), LocalDate.now());
        int days = LocalDate.now().getDayOfMonth();
        int cf = 0, cm = 0, ce = 0, cp = 0;
        for (CheckinModel c : thisCheckins) {
            for (VictoryCheckinModel vc : c.getVictoryCheckins()) {
                if (!vc.isCompleted()) continue;
                switch (vc.getVictory().getCategory()) {
                    case FISICA -> cf++;
                    case MENTAL -> cm++;
                    case ESPIRITUAL -> ce++;
                    case PESSOAL -> cp++;
                }
            }
        }
        Map<String, Integer> completion = new LinkedHashMap<>();
        completion.put("fisica", pct(cf, days));
        completion.put("mental", pct(cm, days));
        completion.put("espiritual", pct(ce, days));
        completion.put("pessoal", pct(cp, days));

        long totalVictories = checkinRepository.findByUserOrderByDateDesc(user).stream()
                .flatMap(c -> c.getVictoryCheckins().stream())
                .filter(VictoryCheckinModel::isCompleted).count();
        long activeDays = checkinRepository.countActiveDays(user);

        var progressCurrent = new ProgressStatsDto.ProgressCurrentDto(
                user.getStreakCurrent(), user.getStreakRecord(), totalVictories, activeDays
        );

        return new ProgressStatsDto(monthDtos, progressCurrent, completion);
    }

    @Override
    public HeatmapDataDto getHeatmap(UserModel user) {
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(27);
        List<CheckinModel> checkins = checkinRepository
                .findByUserAndDateBetweenOrderByDateAsc(user, start, today);
        Map<LocalDate, CheckinModel> byDate = new HashMap<>();
        checkins.forEach(c -> byDate.put(c.getDate(), c));

        List<HeatmapDayDto> days = new ArrayList<>();
        for (int i = 27; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            CheckinModel c = byDate.get(date);
            Map<String, Boolean> victories = new LinkedHashMap<>();
            for (VictoryCategory cat : VictoryCategory.values()) victories.put(cat.name().toLowerCase(), false);
            if (c != null) {
                c.getVictoryCheckins().stream()
                        .filter(VictoryCheckinModel::isCompleted)
                        .forEach(vc -> victories.put(vc.getVictory().getCategory().name().toLowerCase(), true));
            }
            days.add(new HeatmapDayDto(date.toString(), victories));
        }
        return new HeatmapDataDto(days);
    }

    private int pct(int count, int total) {
        return total == 0 ? 0 : (int) Math.round((double) count / total * 100);
    }
}
