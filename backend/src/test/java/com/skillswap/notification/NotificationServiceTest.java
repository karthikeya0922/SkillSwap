package com.skillswap.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.skillswap.common.exception.ForbiddenException;
import com.skillswap.notification.NotificationService.NotificationDto;
import com.skillswap.user.User;
import com.skillswap.user.UserRepository;

/** Mockito tests for storing and delivering notifications, with the async pusher mocked out. */
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

	@Mock
	private NotificationRepository notificationRepository;
	@Mock
	private UserRepository userRepository;
	@Mock
	private NotificationPusher pusher;

	@InjectMocks
	private NotificationService notificationService;

	private User recipient;

	@BeforeEach
	void setUp() {
		recipient = new User();
		recipient.setId(1L);
		recipient.setEmail("asha@test.dev");
	}

	@Test
	void notifyStoresTheNotificationAndPushesItToTheRecipient() {
		when(userRepository.findById(1L)).thenReturn(Optional.of(recipient));

		notificationService.notify(1L, NotificationType.CONNECTION_REQUEST, "New request", "Ravi wants to connect",
				"/connections", null);

		ArgumentCaptor<Notification> stored = ArgumentCaptor.forClass(Notification.class);
		verify(notificationRepository).save(stored.capture());
		assertThat(stored.getValue().getRecipient()).isSameAs(recipient);
		assertThat(stored.getValue().isRead()).isFalse();

		ArgumentCaptor<NotificationDto> pushed = ArgumentCaptor.forClass(NotificationDto.class);
		verify(pusher).push(eq("asha@test.dev"), pushed.capture());
		assertThat(pushed.getValue().title()).isEqualTo("New request");
		assertThat(pushed.getValue().actor()).isNull();
	}

	@Test
	void longTitlesAndMessagesAreTruncatedToTheColumnSizes() {
		when(userRepository.findById(1L)).thenReturn(Optional.of(recipient));

		notificationService.notify(1L, NotificationType.NEW_MESSAGE, "t".repeat(200), "m".repeat(800), null, null);

		ArgumentCaptor<Notification> stored = ArgumentCaptor.forClass(Notification.class);
		verify(notificationRepository).save(stored.capture());
		assertThat(stored.getValue().getTitle()).hasSize(120).endsWith("…");
		assertThat(stored.getValue().getMessage()).hasSize(500).endsWith("…");
	}

	@Test
	void unknownRecipientIsIgnored() {
		when(userRepository.findById(99L)).thenReturn(Optional.empty());

		notificationService.notify(99L, NotificationType.NEW_MESSAGE, "Hi", "Hello", null, null);

		verify(notificationRepository, never()).save(any());
		verify(pusher, never()).push(anyString(), any());
	}

	@Test
	void usersCannotMarkSomeoneElsesNotificationAsRead() {
		Notification notification = new Notification();
		notification.setRecipient(recipient);
		when(notificationRepository.findById(5L)).thenReturn(Optional.of(notification));

		assertThatThrownBy(() -> notificationService.markRead(2L, 5L)).isInstanceOf(ForbiddenException.class);
		assertThat(notification.isRead()).isFalse();

		notificationService.markRead(1L, 5L);
		assertThat(notification.isRead()).isTrue();
	}
}
