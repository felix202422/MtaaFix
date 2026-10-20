package com.mtaafix.notification.service;

import com.mtaafix.notification.domain.NotificationPreference;
import com.mtaafix.notification.dto.NotificationPreferenceDto;
import com.mtaafix.notification.dto.NotificationPreferenceRequest;
import com.mtaafix.notification.repository.NotificationPreferenceRepository;
import com.mtaafix.user.domain.User;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/** FR-6.4: notification preferences (per-user channel opt-in/out). */
@Service
public class NotificationPreferenceService {

    private final NotificationPreferenceRepository preferenceRepository;

    public NotificationPreferenceService(NotificationPreferenceRepository preferenceRepository) {
        this.preferenceRepository = preferenceRepository;
    }

    /** Returns the user's preferences, creating the default row on first use. */
    public NotificationPreference preferenceFor(User user) {
        return preferenceRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    try {
                        return preferenceRepository.saveAndFlush(new NotificationPreference(user));
                    } catch (DataIntegrityViolationException ex) {
                        return preferenceRepository.findByUserId(user.getId())
                                .orElseThrow(() -> ex);
                    }
                });
    }

    public NotificationPreferenceDto get(User user, boolean pushAvailable) {
        return NotificationPreferenceDto.from(preferenceFor(user), pushAvailable);
    }

    public NotificationPreferenceDto update(User user, NotificationPreferenceRequest request,
            boolean pushAvailable) {
        NotificationPreference preference = preferenceFor(user);
        if (request.emailEnabled() != null) {
            preference.setEmailEnabled(request.emailEnabled());
        }
        if (request.pushEnabled() != null) {
            preference.setPushEnabled(request.pushEnabled());
        }
        return NotificationPreferenceDto.from(preferenceRepository.save(preference), pushAvailable);
    }
}
