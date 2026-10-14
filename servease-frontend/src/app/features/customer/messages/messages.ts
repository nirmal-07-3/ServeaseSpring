import { CommonModule } from '@angular/common';
import {
  Component,
  OnInit
} from '@angular/core';
import {
  FormsModule
} from '@angular/forms';
import {
  finalize
} from 'rxjs';

import { Api } from '../../../core/services/api';

interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}

interface Message {
  id: number;
  bookingId: number;
  senderId: number;
  senderName: string;
  recipientId: number;
  recipientName: string;
  content: string;
  createdAt: string;
}

interface Conversation {
  bookingId: number;
  otherUserId: number;
  otherUserName: string;
  lastMessage: string;
  lastMessageTime: string;
}

@Component({
  selector: 'app-messages',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule
  ],
  templateUrl: './messages.html',
  styleUrl: './messages.css'
})
export class Messages implements OnInit {

  messages: Message[] = [];
  conversations: Conversation[] = [];

  selectedBookingId: number | null = null;
  selectedConversation: Conversation | null = null;

  newMessage = '';

  loading = true;
  conversationLoading = false;
  sending = false;

  errorMessage = '';
  sendError = '';

  private currentUserId: number | null = null;

  constructor(
    private api: Api
  ) {}

  ngOnInit(): void {
    this.loadMessages();
  }

  loadMessages(): void {

    this.loading = true;
    this.errorMessage = '';

    this.api
      .get<ApiResponse<Message[]>>('/messages')
      .pipe(
        finalize(() => {
          this.loading = false;
        })
      )
      .subscribe({
        next: (response) => {

          this.messages = response.data ?? [];

          this.buildConversations();

          if (
            this.selectedBookingId !== null
          ) {
            const conversation =
              this.conversations.find(
                item =>
                  item.bookingId ===
                  this.selectedBookingId
              );

            if (conversation) {
              this.selectedConversation =
                conversation;
            }
          }

          if (
            this.selectedBookingId === null &&
            this.conversations.length > 0
          ) {
            this.selectConversation(
              this.conversations[0]
            );
          }
        },

        error: (error) => {

          this.errorMessage =
            error?.error?.message ||
            'Unable to load messages.';
        }
      });
  }

  buildConversations(): void {

    const conversationMap =
      new Map<number, Conversation>();

    const sortedMessages =
      [...this.messages].sort(
        (a, b) =>
          new Date(a.createdAt).getTime() -
          new Date(b.createdAt).getTime()
      );

    for (const message of sortedMessages) {

      if (this.currentUserId === null) {
        this.currentUserId =
          message.senderId;
      }

      const isSender =
        message.senderId ===
        this.currentUserId;

      const otherUserId =
        isSender
          ? message.recipientId
          : message.senderId;

      const otherUserName =
        isSender
          ? message.recipientName
          : message.senderName;

      conversationMap.set(
        message.bookingId,
        {
          bookingId:
          message.bookingId,

          otherUserId:
          otherUserId,

          otherUserName:
          otherUserName,

          lastMessage:
          message.content,

          lastMessageTime:
          message.createdAt
        }
      );
    }

    this.conversations =
      Array.from(
        conversationMap.values()
      ).sort(
        (a, b) =>
          new Date(
            b.lastMessageTime
          ).getTime() -
          new Date(
            a.lastMessageTime
          ).getTime()
      );
  }

  selectConversation(
    conversation: Conversation
  ): void {

    this.selectedConversation =
      conversation;

    this.selectedBookingId =
      conversation.bookingId;

    this.loadConversation(
      conversation.bookingId
    );
  }

  loadConversation(
    bookingId: number
  ): void {

    this.conversationLoading = true;
    this.sendError = '';

    this.api
      .get<ApiResponse<Message[]>>(
        `/messages/booking/${bookingId}`
      )
      .pipe(
        finalize(() => {
          this.conversationLoading =
            false;
        })
      )
      .subscribe({
        next: (response) => {

          this.messages =
            this.messages.filter(
              message =>
                message.bookingId !==
                bookingId
            );

          this.messages.push(
            ...(response.data ?? [])
          );

          this.messages =
            [...this.messages].sort(
              (a, b) =>
                new Date(
                  a.createdAt
                ).getTime() -
                new Date(
                  b.createdAt
                ).getTime()
            );
        },

        error: (error) => {

          this.sendError =
            error?.error?.message ||
            'Unable to load this conversation.';
        }
      });
  }

  get selectedMessages(): Message[] {

    if (
      this.selectedBookingId === null
    ) {
      return [];
    }

    return this.messages.filter(
      message =>
        message.bookingId ===
        this.selectedBookingId
    );
  }

  sendMessage(): void {

    const content =
      this.newMessage.trim();

    if (
      !content ||
      this.selectedBookingId === null ||
      this.sending
    ) {
      return;
    }

    this.sending = true;
    this.sendError = '';

    const body = {
      bookingId:
      this.selectedBookingId,
      content
    };

    this.api
      .post<ApiResponse<Message>>(
        '/messages',
        body
      )
      .pipe(
        finalize(() => {
          this.sending = false;
        })
      )
      .subscribe({
        next: (response) => {

          if (response.data) {
            this.messages.push(
              response.data
            );
          }

          this.newMessage = '';

          this.updateConversationPreview(
            response.data
          );
        },

        error: (error) => {

          this.sendError =
            error?.error?.message ||
            'Unable to send message.';
        }
      });
  }

  updateConversationPreview(
    message: Message
  ): void {

    const index =
      this.conversations.findIndex(
        conversation =>
          conversation.bookingId ===
          message.bookingId
      );

    if (index === -1) {
      return;
    }

    const conversation =
      this.conversations[index];

    conversation.lastMessage =
      message.content;

    conversation.lastMessageTime =
      message.createdAt;

    this.conversations =
      [
        conversation,
        ...this.conversations.filter(
          (_, i) => i !== index
        )
      ];
  }

  handleEnter(
    event: KeyboardEvent
  ): void {

    if (
      event.key === 'Enter' &&
      !event.shiftKey
    ) {
      event.preventDefault();
      this.sendMessage();
    }
  }

  formatTime(
    dateString: string
  ): string {

    if (!dateString) {
      return '';
    }

    return new Date(
      dateString
    ).toLocaleTimeString(
      [],
      {
        hour: '2-digit',
        minute: '2-digit'
      }
    );
  }

  formatDate(
    dateString: string
  ): string {

    if (!dateString) {
      return '';
    }

    return new Date(
      dateString
    ).toLocaleDateString(
      [],
      {
        day: '2-digit',
        month: 'short'
      }
    );
  }

  isMyMessage(
    message: Message
  ): boolean {

    if (this.currentUserId === null) {
      return false;
    }

    return (
      message.senderId ===
      this.currentUserId
    );
  }

  getInitials(
    name: string
  ): string {

    if (!name) {
      return '?';
    }

    const parts =
      name.trim().split(/\s+/);

    if (parts.length === 1) {
      return parts[0]
        .substring(0, 2)
        .toUpperCase();
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

  trackByMessage(
    index: number,
    message: Message
  ): number {

    return message.id;
  }
}
