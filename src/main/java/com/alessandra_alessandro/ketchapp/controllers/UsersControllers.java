package com.alessandra_alessandro.ketchapp.controllers;

import com.alessandra_alessandro.ketchapp.models.dto.*;
import com.alessandra_alessandro.ketchapp.models.entity.*;
import com.alessandra_alessandro.ketchapp.repositories.AchievementsRepository;
import com.alessandra_alessandro.ketchapp.repositories.UsersRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UsersControllers {

    private final UsersRepository usersRepository;
    private final AchievementsRepository achievementsRepository;

    @Autowired
    public UsersControllers(UsersRepository usersRepository, AchievementsRepository achievementsRepository) {
        this.usersRepository = usersRepository;
        this.achievementsRepository = achievementsRepository;
    }

    private UserDto convertEntityToDto(UserEntity entity) {
        if (entity == null) {
            return null;
        }
        UserDto dto = new UserDto();
        dto.setUuid(entity.getUuid());
        dto.setUsername(entity.getUsername());
        return dto;
    }

    private UserEntity convertDtoToEntity(UserDto dto) {
        if (dto == null) {
            return null;
        }
        return new UserEntity(
                dto.getUuid(),
                dto.getUsername()
        );
    }

    @Transactional
    public UserDto createUser(UserDto userDto) {
        if (userDto == null) {
            throw new IllegalArgumentException("UserDto cannot be null");
        }
        if (usersRepository.findByUsername(userDto.getUsername()).isPresent()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "User already exists"
            );
        }
        UserEntity userEntity = convertDtoToEntity(userDto);
        userEntity = usersRepository.save(userEntity);
        return convertEntityToDto(userEntity);
    }

    public UserDto getUser(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("UUID cannot be null");
        }
        Optional<UserEntity> entityOptional = usersRepository.findById(id);
        if (entityOptional.isPresent()) {
            return convertEntityToDto(entityOptional.get());
        } else {
            throw new IllegalArgumentException("User with UUID '" + id + "' not found.");
        }
    }

    public String getEmailByUsername(String username) {
        if (username == null || username.isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }
        Optional<UserEntity> entityOptional = usersRepository.findByUsername(username);
        if (entityOptional.isPresent()) {
            return entityOptional.get().getEmail();
        } else {
            throw new IllegalArgumentException("User with username '" + username + "' not found.");
        }
    }

    public List<TomatoDto> getUserTomatoes(UUID uuid) {
        if (uuid == null) {
            throw new IllegalArgumentException("UUID cannot be null");
        }
        List<TomatoEntity> tomatoes = usersRepository.findTomatoesByUuid(uuid);
        return tomatoes.stream()
                .map(tomato -> new TomatoDto(
                        tomato.getId(),
                        tomato.getUserUUID(),
                        tomato.getStartAt(),
                        tomato.getEndAt(),
                        tomato.getPauseEnd(),
                        tomato.getNextTomatoId(),
                        tomato.getSubject(),
                        tomato.getCreatedAt()
                )).collect(Collectors.toList());
    }

    public List<TomatoDto> getUserTomatoes(UUID uuid, LocalDate date) {
        if (uuid == null) {
            throw new IllegalArgumentException("UUID cannot be null");
        }
        List<TomatoEntity> tomatoes = usersRepository.findTomatoesByUuid(uuid);
        return tomatoes.stream()
                .filter(tomato -> date == null || !tomato.getCreatedAt().toLocalDateTime().toLocalDate().isBefore(date))
                .map(tomato -> new TomatoDto(
                        tomato.getId(),
                        tomato.getUserUUID(),
                        tomato.getStartAt(),
                        tomato.getEndAt(),
                        tomato.getPauseEnd(),
                        tomato.getNextTomatoId(),
                        tomato.getSubject(),
                        tomato.getCreatedAt()
                )).collect(Collectors.toList());
    }

    public List<TomatoDto> getUserTomatoes(UUID uuid, LocalDate startDate, LocalDate endDate) {
        if (uuid == null) {
            throw new IllegalArgumentException("UUID cannot be null");
        }
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Both startDate and endDate must be provided");
        }
        List<TomatoEntity> tomatoes = usersRepository.findTomatoesByUuid(uuid);
        return tomatoes.stream()
                .filter(tomato -> {
                    LocalDate tomatoDate = tomato.getCreatedAt().toLocalDateTime().toLocalDate();
                    return (tomatoDate.isEqual(startDate) || tomatoDate.isAfter(startDate)) &&
                            (tomatoDate.isEqual(endDate) || tomatoDate.isBefore(endDate));
                })
                .map(tomato -> new TomatoDto(
                        tomato.getId(),
                        tomato.getUserUUID(),
                        tomato.getStartAt(),
                        tomato.getEndAt(),
                        tomato.getPauseEnd(),
                        tomato.getNextTomatoId(),
                        tomato.getSubject(),
                        tomato.getCreatedAt()
                )).collect(Collectors.toList());
    }

    public List<ActivityDto> getUserActivities(UUID uuid) {
        if (uuid == null) {
            throw new IllegalArgumentException("UUID cannot be null");
        }
        List<ActivityEntity> activities = usersRepository.findActivitiesByUuid(uuid);
        return activities.stream()
                .map(activity -> new ActivityDto(
                        activity.getId(),
                        activity.getUserUUID(),
                        activity.getTomatoId(),
                        activity.getType(),
                        activity.getAction(),
                        activity.getCreatedAt()
                )).collect(Collectors.toList());
    }

    public List<AchievementDto> getUserAchievements(UUID uuid) {
        if (uuid == null) {
            throw new IllegalArgumentException("UUID cannot be null");
        }
        if (usersRepository.findById(uuid).isEmpty()) {
            return new ArrayList<>();
        }

        String studiedIcon = "https://cdn-icons-png.flaticon.com/512/3068/3068380.png";
        Double totalHours = usersRepository.findTotalHoursByUserUUID(uuid);
        if (totalHours == null) totalHours = 0.0;
        Optional<AchievementEntity> studiedAchievement = usersRepository.findAchievementsByUuid(uuid).stream()
                .filter(a -> "Studied for 5 hours".equals(a.getDescription())).findFirst();
        boolean studiedCompleted = totalHours >= 5.0;
        AchievementEntity studiedEntity = studiedAchievement.orElseGet(() -> new AchievementEntity(uuid, "Studied for 5 hours", studiedCompleted, studiedIcon));
        studiedEntity.setCompleted(studiedCompleted);
        studiedEntity.setIcon(studiedIcon);
        achievementsRepository.save(studiedEntity);

        String tomatoIcon = "https://cdn-icons-png.flaticon.com/512/590/590685.png";
        int tomatoCount = Math.toIntExact(usersRepository.countTomatoesByUserUUID(uuid));
        Optional<AchievementEntity> tomatoAchievement = usersRepository.findAchievementsByUuid(uuid).stream()
                .filter(a -> "Completed 10 Tomatoes".equals(a.getDescription())).findFirst();
        boolean tomatoCompleted = tomatoCount >= 10;
        AchievementEntity tomatoEntity = tomatoAchievement.orElseGet(() -> new AchievementEntity(uuid, "Completed 10 Tomatoes", tomatoCompleted, tomatoIcon));
        tomatoEntity.setCompleted(tomatoCompleted);
        tomatoEntity.setIcon(tomatoIcon);
        achievementsRepository.save(tomatoEntity);

        List<AchievementEntity> achievements = usersRepository.findAchievementsByUuid(uuid);
        if (achievements == null) return new ArrayList<>();
        return achievements.stream()
                .map(achievementEntity -> new AchievementDto(
                        achievementEntity.getId(),
                        achievementEntity.getUserUUID(),
                        achievementEntity.getDescription(),
                        achievementEntity.getCompleted(),
                        achievementEntity.getCreatedAt(),
                        achievementEntity.getIcon()
                )).collect(Collectors.toList());
    }

    public UUID getUserUUIDByFirebaseUID(String firebaseUID) {
        if (firebaseUID == null || firebaseUID.isEmpty()) {
            throw new IllegalArgumentException("Firebase UID cannot be null or empty");
        }
        Optional<UserEntity> entityOptional = usersRepository.findByFirebaseUid(firebaseUID);
        if (entityOptional.isPresent()) {
            return entityOptional.get().getUuid();
        } else {
            throw new IllegalArgumentException("User with Firebase UID '" + firebaseUID + "' not found.");
        }
    }

    public StatisticsDto getUserStatistics(UUID uuid, LocalDate startDate, LocalDate endDate) {
        if (uuid == null) {
            throw new IllegalArgumentException("UUID cannot be null");
        }
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Start date and end date cannot be null");
        }
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("End date cannot be before start date");
        }
        List<LocalDate> rangeDates = new ArrayList<>();
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            rangeDates.add(current);
            current = current.plusDays(1);
        }
        List<StatisticsDateDto> statisticsDates = new ArrayList<>();
        for (LocalDate date : rangeDates) {
            List<String> subjects = usersRepository.findSubjectsByUuidAndDate(uuid, date.toString());
            List<StatisticsSubjectDto> statisticsSubjects = new ArrayList<>();
            for (String subject : subjects) {
                Double totalHours = usersRepository.findTotalHoursByUserAndSubjectAndDate(uuid, subject, date.toString());
                statisticsSubjects.add(new StatisticsSubjectDto(subject, totalHours != null ? totalHours : 0.0));
            }
            statisticsDates.add(new StatisticsDateDto(
                    date,
                    statisticsSubjects.stream().mapToDouble(StatisticsSubjectDto::getHours).sum(),
                    statisticsSubjects));
        }
        StatisticsDto statisticsDto = new StatisticsDto();
        statisticsDto.setDates(statisticsDates);
        return statisticsDto;
    }
}

