
import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { finalize, Subscription, timeout } from 'rxjs';

import { Api } from '../../../core/services/api';
import { Auth } from '../../../core/services/auth';

interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}

type BookingStatus =
  | 'PENDING'
  | 'ACCEPTED'
  | 'REJECTED'
  | 'IN_PROGRESS'
  | 'COMPLETED'
  | 'CANCELLED';

interface Message {
  id: number;
  bookingId: number;
  senderId: number;
  senderName: string;
  recipientId: number;
  recipientName: string;
  content: string;
  createdAt: string;
  isRead?: boolean;
}

interface Conversation {
  bookingId: number;
  otherUserId: number;
  otherUserName: string;
  lastMessage: string;
  lastMessageTime: string;
  unreadCount: number;
}

@Component({
  selector: 'app-provider-messages',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './messages.html',
  styleUrl: './messages.css'
})
export class Messages implements OnInit, OnDestroy {
  messages: Message[] = [];
  conversations: Conversation[] = [];

  selectedBookingId: number | null = null;
  selectedConversation: Conversation | null = null;
  selectedBookingStatus: BookingStatus | null = null;

  newMessage = '';

  loading = false;
  conversationLoading = false;
  bookingStatusLoading = false;
  sending = false;

  errorMessage = '';
  sendError = '';
  bookingStatusError = '';

  private currentUserId: number | null;
  readonly currentUserRole: string | null;

  private routeSubscription?: Subscription;
  private inboxRequestId = 0;
  private conversationRequestId = 0;
  private bookingStatusRequestId = 0;

  constructor(
    private api: Api,
    private auth: Auth,
    private route: ActivatedRoute,
    private cdr: ChangeDetectorRef
  ) {
    this.currentUserId = this.auth.getUser()?.id ?? null;
    this.currentUserRole = this.auth.getRole();
  }

  ngOnInit(): void {
    this.routeSubscription = this.route.queryParamMap.subscribe(params => {
      const rawBookingId = params.get('bookingId');
      const bookingId = rawBookingId === null ? null : Number(rawBookingId);

      if (
        bookingId !== null &&
        (!Number.isInteger(bookingId) || bookingId <= 0)
      ) {
        this.errorMessage = 'The booking ID in the URL is invalid.';
        this.loading = false;
        this.cdr.detectChanges();
        return;
      }

      this.selectedBookingId = bookingId;
      this.loadMessages(bookingId);
    });
  }

  ngOnDestroy(): void {
    this.routeSubscription?.unsubscribe();
    this.inboxRequestId++;
    this.conversationRequestId++;
    this.bookingStatusRequestId++;
  }

  get isReadOnly(): boolean {
    return (
      this.selectedBookingStatus === 'COMPLETED' ||
      this.selectedBookingStatus === 'CANCELLED' ||
      this.selectedBookingStatus === 'REJECTED'
    );
  }

  get canSendMessage(): boolean {
    return (
      this.selectedBookingStatus !== null &&
      ['PENDING', 'ACCEPTED', 'IN_PROGRESS'].includes(
        this.selectedBookingStatus
      ) &&
      !this.bookingStatusLoading &&
      !this.bookingStatusError &&
      !this.conversationLoading &&
      !this.sending &&
      !this.sendError
    );
  }

  get readOnlyNotice(): string {
    switch (this.selectedBookingStatus) {
      case 'COMPLETED':
        return 'This booking is completed. Messaging is now read-only.';
      case 'CANCELLED':
        return 'This booking has been cancelled. Conversation history is read-only.';
      case 'REJECTED':
        return 'This booking was rejected. Conversation history is read-only.';
      default:
        return '';
    }
  }

  loadMessages(bookingIdFromRoute: number | null = null): void {
    const requestId = ++this.inboxRequestId;

    this.loading = true;
    this.errorMessage = '';
    this.cdr.detectChanges();

    this.api
      .get<ApiResponse<Message[]>>('/messages')
      .pipe(
        timeout(15000),
        finalize(() => {
          if (requestId === this.inboxRequestId) {
            this.loading = false;
            this.cdr.detectChanges();
          }
        })
      )
      .subscribe({
        next: response => {
          if (requestId !== this.inboxRequestId) {
            return;
          }

          this.messages = Array.isArray(response?.data)
            ? response.data
            : [];

          this.buildConversations();

          const bookingId = bookingIdFromRoute ?? this.selectedBookingId;

          if (bookingId !== null) {
            this.openBookingConversation(bookingId);
          } else if (this.conversations.length > 0) {
            this.selectConversation(this.conversations[0]);
          } else {
            this.selectedBookingId = null;
            this.selectedConversation = null;
            this.selectedBookingStatus = null;
            this.conversationLoading = false;
            this.bookingStatusLoading = false;
          }

          this.cdr.detectChanges();
        },
        error: error => {
          if (requestId !== this.inboxRequestId) {
            return;
          }

          this.errorMessage =
            error?.name === 'TimeoutError'
              ? 'Loading messages timed out. Please try again.'
              : error?.error?.message ||
              'Unable to load messages. Please try again.';

          this.cdr.detectChanges();
        }
      });
  }

  private createPlaceholderConversation(bookingId: number): Conversation {
    return {
      bookingId,
      otherUserId: 0,
      otherUserName: 'Customer',
      lastMessage: 'Start a conversation about this booking',
      lastMessageTime: '',
      unreadCount: 0
    };
  }

  buildConversations(): void {
    const conversationMap = new Map<number, Conversation>();

    const sortedMessages = [...this.messages].sort(
      (a, b) =>
        new Date(a.createdAt).getTime() -
        new Date(b.createdAt).getTime()
    );

    for (const message of sortedMessages) {
      const isSender = message.senderId === this.currentUserId;
      const otherUserId = isSender
        ? message.recipientId
        : message.senderId;
      const otherUserName = isSender
        ? message.recipientName
        : message.senderName;

      const unreadCount = sortedMessages.filter(item =>
        item.bookingId === message.bookingId &&
        item.recipientId === this.currentUserId &&
        item.isRead === false
      ).length;

      const existing = conversationMap.get(message.bookingId);

      conversationMap.set(message.bookingId, {
        bookingId: message.bookingId,
        otherUserId,
        otherUserName,
        lastMessage: message.content,
        lastMessageTime: message.createdAt,
        unreadCount: existing?.unreadCount ?? unreadCount
      });
    }

    this.conversations = Array.from(conversationMap.values()).sort(
      (a, b) =>
        new Date(b.lastMessageTime).getTime() -
        new Date(a.lastMessageTime).getTime()
    );

    this.cdr.detectChanges();
  }

  openBookingConversation(bookingId: number): void {
    if (!Number.isInteger(bookingId) || bookingId <= 0) {
      this.errorMessage = 'Invalid booking ID.';
      this.cdr.detectChanges();
      return;
    }

    this.selectedBookingId = bookingId;
    this.selectedBookingStatus = null;
    this.bookingStatusError = '';
    this.bookingStatusLoading = true;

    this.selectedConversation =
      this.conversations.find(item => item.bookingId === bookingId) ??
      this.createPlaceholderConversation(bookingId);

    this.sendError = '';
    this.newMessage = '';
    this.cdr.detectChanges();

    this.loadBookingStatus(bookingId);
    this.loadConversation(bookingId);
  }

  selectConversation(conversation: Conversation): void {
    this.openBookingConversation(conversation.bookingId);
  }

  private loadBookingStatus(bookingId: number): void {
    const requestId = ++this.bookingStatusRequestId;

    if (!Number.isInteger(bookingId) || bookingId <= 0) {
      this.selectedBookingStatus = null;
      this.bookingStatusError = 'No valid booking was selected.';
      this.bookingStatusLoading = false;
      this.cdr.detectChanges();
      return;
    }

    this.bookingStatusLoading = true;
    this.bookingStatusError = '';
    this.cdr.detectChanges();

    this.api
      .get<ApiResponse<string>>(`/bookings/${bookingId}/messaging-status`)
      .pipe(
        timeout(15000),
        finalize(() => {
          if (
            requestId === this.bookingStatusRequestId &&
            bookingId === this.selectedBookingId
          ) {
            this.bookingStatusLoading = false;
            this.cdr.detectChanges();
          }
        })
      )
      .subscribe({
        next: response => {
          if (
            requestId !== this.bookingStatusRequestId ||
            bookingId !== this.selectedBookingId
          ) {
            return;
          }

          const status = String(response?.data ?? '')
            .trim()
            .toUpperCase();

          const validStatuses: BookingStatus[] = [
            'PENDING',
            'ACCEPTED',
            'REJECTED',
            'IN_PROGRESS',
            'COMPLETED',
            'CANCELLED'
          ];

          if (
            !response?.success ||
            !validStatuses.includes(status as BookingStatus)
          ) {
            this.selectedBookingStatus = null;
            this.bookingStatusError =
              'Unable to verify this booking status. Please retry.';
            this.cdr.detectChanges();
            return;
          }

          this.selectedBookingStatus = status as BookingStatus;
          this.bookingStatusError = '';
          this.cdr.detectChanges();
        },
        error: error => {
          if (
            requestId !== this.bookingStatusRequestId ||
            bookingId !== this.selectedBookingId
          ) {
            return;
          }

          console.error('Failed to verify booking status:', error);
          this.selectedBookingStatus = null;
          this.bookingStatusError =
            error?.name === 'TimeoutError'
              ? 'Checking booking status timed out. Please retry.'
              : 'Unable to verify this booking status. Please retry.';
          this.cdr.detectChanges();
        }
      });
  }

  loadConversation(bookingId: number): void {
    const requestId = ++this.conversationRequestId;

    this.selectedBookingId = bookingId;
    this.conversationLoading = true;
    this.sendError = '';
    this.cdr.detectChanges();

    this.api
      .get<ApiResponse<Message[]>>(`/messages/booking/${bookingId}`)
      .pipe(
        timeout(15000),
        finalize(() => {
          if (requestId === this.conversationRequestId) {
            this.conversationLoading = false;
            this.cdr.detectChanges();
          }
        })
      )
      .subscribe({
        next: response => {
          if (
            requestId !== this.conversationRequestId ||
            bookingId !== this.selectedBookingId
          ) {
            return;
          }

          const bookingMessages = Array.isArray(response?.data)
            ? response.data
            : [];

          this.messages = [
            ...this.messages.filter(
              message => message.bookingId !== bookingId
            ),
            ...bookingMessages
          ].sort(
            (a, b) =>
              new Date(a.createdAt).getTime() -
              new Date(b.createdAt).getTime()
          );

          this.buildConversations();

          this.selectedConversation =
            this.conversations.find(
              item => item.bookingId === bookingId
            ) ?? this.createPlaceholderConversation(bookingId);

          this.markConversationAsRead(bookingId);
          this.cdr.detectChanges();
        },
        error: error => {
          if (
            requestId !== this.conversationRequestId ||
            bookingId !== this.selectedBookingId
          ) {
            return;
          }

          this.selectedConversation =
            this.conversations.find(
              item => item.bookingId === bookingId
            ) ?? this.createPlaceholderConversation(bookingId);

          this.sendError =
            error?.name === 'TimeoutError'
              ? 'Loading timed out. Retry opening this conversation.'
              : error?.error?.message ||
              'Could not load this conversation. Check your booking access.';

          this.cdr.detectChanges();
        }
      });
  }

  private markConversationAsRead(bookingId: number): void {
    if (
      !Number.isInteger(bookingId) ||
      bookingId <= 0 ||
      bookingId !== this.selectedBookingId
    ) {
      return;
    }

    this.api
      .patch<ApiResponse<number>>(`/messages/booking/${bookingId}/read`)
      .pipe(
        timeout(15000),
        finalize(() => {
          this.cdr.detectChanges();
        })
      )
      .subscribe({
        next: response => {
          if (
            bookingId !== this.selectedBookingId ||
            !response?.success
          ) {
            return;
          }

          this.messages = this.messages.map(message =>
            message.bookingId === bookingId &&
            message.recipientId === this.currentUserId
              ? { ...message, isRead: true }
              : message
          );

          this.buildConversations();
          this.cdr.detectChanges();
        },
        error: error => {
          console.error('Unable to mark messages as read:', error);
        }
      });
  }

  get selectedMessages(): Message[] {
    if (this.selectedBookingId === null) {
      return [];
    }

    return this.messages.filter(
      message => message.bookingId === this.selectedBookingId
    );
  }

  sendMessage(): void {
    const content = this.newMessage.trim();
    const bookingId = this.selectedBookingId;

    if (
      !content ||
      bookingId === null ||
      this.sending ||
      !this.canSendMessage
    ) {
      return;
    }

    if (content.length > 2000) {
      this.sendError = 'Messages cannot exceed 2000 characters.';
      this.cdr.detectChanges();
      return;
    }

    this.sending = true;
    this.sendError = '';
    this.cdr.detectChanges();

    this.api
      .post<ApiResponse<Message>>('/messages', {
        bookingId,
        content
      })
      .pipe(
        timeout(15000),
        finalize(() => {
          this.sending = false;
          this.cdr.detectChanges();
        })
      )
      .subscribe({
        next: response => {
          if (bookingId !== this.selectedBookingId) {
            return;
          }

          if (!response?.data) {
            this.sendError =
              'The server did not return the sent message. Refresh the conversation.';
            this.cdr.detectChanges();
            return;
          }

          this.messages = [
            ...this.messages.filter(
              message => message.id !== response.data.id
            ),
            response.data
          ].sort(
            (a, b) =>
              new Date(a.createdAt).getTime() -
              new Date(b.createdAt).getTime()
          );

          this.newMessage = '';
          this.buildConversations();

          this.selectedConversation =
            this.conversations.find(
              item => item.bookingId === bookingId
            ) ?? this.createPlaceholderConversation(bookingId);

          this.cdr.detectChanges();
        },
        error: error => {
          this.sendError =
            error?.name === 'TimeoutError'
              ? 'Sending timed out. Check the conversation before retrying.'
              : error?.error?.message ||
              'Unable to send this message. Verify your access to the booking.';

          this.cdr.detectChanges();
        }
      });
  }

  retryConversation(): void {
    if (this.selectedBookingId !== null) {
      this.sendError = '';
      this.loadBookingStatus(this.selectedBookingId);
      this.loadConversation(this.selectedBookingId);
    }
  }

  retry(): void {
    if (!this.loading) {
      this.loadMessages(this.selectedBookingId);
    }
  }

  handleEnter(event: KeyboardEvent): void {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.sendMessage();
    }
  }

  isMyMessage(message: Message): boolean {
    return (
      this.currentUserId !== null &&
      message.senderId === this.currentUserId
    );
  }

  formatTime(dateString: string): string {
    if (!dateString) {
      return '';
    }

    return new Date(dateString).toLocaleTimeString([], {
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  formatDate(dateString: string): string {
    if (!dateString) {
      return '';
    }

    return new Date(dateString).toLocaleDateString([], {
      day: '2-digit',
      month: 'short'
    });
  }

  getInitials(name: string): string {
    if (!name?.trim()) {
      return '?';
    }

    const parts = name.trim().split(/\s+/);

    if (parts.length === 1) {
      return parts[0].substring(0, 2).toUpperCase();
    }

    return (
      parts[0][0] +
      parts[parts.length - 1][0]
    ).toUpperCase();
  }

  trackByConversation(
    index: number,
    conversation: Conversation
  ): number {
    return conversation.bookingId;
  }

  trackByMessage(index: number, message: Message): number {
    return message.id;
  }
}
