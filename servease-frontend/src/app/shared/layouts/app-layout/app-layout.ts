
import { ChangeDetectorRef, Component, OnDestroy } from '@angular/core';
import { NavigationEnd, Router, RouterLink, RouterOutlet } from '@angular/router';
import { filter, finalize, Subscription } from 'rxjs';
import { CommonModule } from '@angular/common';
import { Sidebar } from '../../components/sidebar/sidebar';
import { Auth } from '../../../core/services/auth';
import {
  AppNotification,
  NotificationService
} from '../../../core/services/notification';

@Component({
  selector: 'app-app-layout',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, Sidebar],
  templateUrl: './app-layout.html',
  styleUrl: './app-layout.css'
})
export class AppLayout implements OnDestroy {
  sidebarOpen = false;

  userName = 'User';
  userRole = 'CUSTOMER';
  currentUrl = '';

  notificationsOpen = false;
  notifications: AppNotification[] = [];
  unreadCount = 0;

  notificationsLoading = false;
  unreadCountLoading = false;
  markingAllAsRead = false;

  markingReadIds = new Set<number>();
  notificationError = '';

  private routerSubscription?: Subscription;

  constructor(
    private auth: Auth,
    private router: Router,
    private notificationService: NotificationService,
    private cdr: ChangeDetectorRef
  ) {
    const user = this.auth.getUser();

    if (user?.name) {
      this.userName = user.name;
    }

    this.userRole = (this.auth.getRole() || 'CUSTOMER').toUpperCase();
    this.currentUrl = this.router.url;

    this.routerSubscription = this.router.events
      .pipe(filter(event => event instanceof NavigationEnd))
      .subscribe(() => {
        this.currentUrl = this.router.url;
        this.closeSidebar();
        this.notificationsOpen = false;
        this.refreshView();
      });

    this.loadUnreadCount();
  }

  ngOnDestroy(): void {
    this.routerSubscription?.unsubscribe();
  }

  private refreshView(): void {
    this.cdr.markForCheck();
  }

  toggleSidebar(): void {
    this.sidebarOpen = !this.sidebarOpen;
  }

  closeSidebar(): void {
    this.sidebarOpen = false;
  }

  toggleNotifications(): void {
    this.notificationsOpen = !this.notificationsOpen;

    if (this.notificationsOpen) {
      this.loadNotifications();
      this.loadUnreadCount();
    }
  }

  closeNotifications(): void {
    this.notificationsOpen = false;
  }

  loadNotifications(): void {
    this.notificationsLoading = true;
    this.notificationError = '';
    this.refreshView();

    this.notificationService.getNotifications()
      .pipe(
        finalize(() => {
          this.notificationsLoading = false;
          this.refreshView();
        })
      )
      .subscribe({
        next: notifications => {
          this.notifications = notifications;
          this.refreshView();
        },
        error: () => {
          this.notificationError =
            'Unable to load notifications. Please try again.';
          this.refreshView();
        }
      });
  }

  loadUnreadCount(): void {
    this.unreadCountLoading = true;
    this.refreshView();

    this.notificationService.getUnreadCount()
      .pipe(
        finalize(() => {
          this.unreadCountLoading = false;
          this.refreshView();
        })
      )
      .subscribe({
        next: response => {
          this.unreadCount = response.unreadCount;
          this.refreshView();
        },
        error: () => {
          this.unreadCount = 0;
          this.refreshView();
        }
      });
  }

  markAsRead(
    notification: AppNotification,
    event?: MouseEvent
  ): void {
    event?.stopPropagation();

    if (
      notification.read ||
      this.markingReadIds.has(notification.id)
    ) {
      return;
    }

    this.markingReadIds.add(notification.id);
    this.refreshView();

    this.notificationService.markAsRead(notification.id)
      .pipe(
        finalize(() => {
          this.markingReadIds.delete(notification.id);
          this.refreshView();
        })
      )
      .subscribe({
        next: updatedNotification => {
          this.notifications = this.notifications.map(item =>
            item.id === updatedNotification.id
              ? updatedNotification
              : item
          );

          this.loadUnreadCount();
          this.refreshView();
        },
        error: () => {
          this.notificationError =
            'Unable to mark the notification as read.';
          this.refreshView();
        }
      });
  }

  markAllAsRead(): void {
    if (this.markingAllAsRead || this.unreadCount === 0) {
      return;
    }

    this.markingAllAsRead = true;
    this.notificationError = '';
    this.refreshView();

    this.notificationService.markAllAsRead()
      .pipe(
        finalize(() => {
          this.markingAllAsRead = false;
          this.refreshView();
        })
      )
      .subscribe({
        next: () => {
          this.notifications = this.notifications.map(notification => ({
            ...notification,
            read: true
          }));

          this.unreadCount = 0;
          this.refreshView();
        },
        error: () => {
          this.notificationError =
            'Unable to mark all notifications as read.';
          this.refreshView();
        }
      });
  }

  openNotification(notification: AppNotification): void {
    if (!notification.read) {
      this.markAsRead(notification);
    }

    this.notificationsOpen = false;

    if (notification.targetUrl) {
      void this.router.navigateByUrl(notification.targetUrl);
    }

    this.refreshView();
  }

  formatNotificationDate(date: string): string {
    const notificationDate = new Date(date);

    if (Number.isNaN(notificationDate.getTime())) {
      return '';
    }

    return notificationDate.toLocaleString([], {
      day: 'numeric',
      month: 'short',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  getNotificationIcon(type: string): string {
    switch (type) {
      case 'BOOKING_CREATED':
        return '＋';

      case 'BOOKING_ACCEPTED':
      case 'SERVICE_COMPLETED':
      case 'PROVIDER_APPROVED':
        return '✓';

      case 'BOOKING_REJECTED':
      case 'BOOKING_CANCELLED':
      case 'PAYMENT_FAILED':
      case 'PAYMENT_REQUIRES_ATTENTION':
      case 'PROVIDER_REJECTED':
        return '!';

      case 'PAYMENT_SUCCESS':
        return '₹';

      case 'SERVICE_STARTED':
        return '▶';

      default:
        return '•';
    }
  }

  getUserInitials(): string {
    if (!this.userName.trim()) {
      return 'U';
    }

    const parts = this.userName.trim().split(/\s+/);

    if (parts.length === 1) {
      return parts[0].charAt(0).toUpperCase();
    }

    return (
      parts[0].charAt(0) +
      parts[parts.length - 1].charAt(0)
    ).toUpperCase();
  }
}
