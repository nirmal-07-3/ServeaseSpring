
import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface AppNotification {
  id: number;
  type: string;
  title: string;
  message: string;
  targetUrl: string | null;
  referenceType: string | null;
  referenceId: number | null;
  read: boolean;
  createdAt: string;
}

export interface NotificationPage {
  notifications: AppNotification[];
  unreadCount: number;
}

@Injectable({
  providedIn: 'root'
})
export class NotificationService {
  private readonly apiUrl = '/api/notifications';

  constructor(private http: HttpClient) {}

  getNotifications(page = 0, size = 20): Observable<AppNotification[]> {
    const params = new HttpParams()
      .set('page', page)
      .set('size', size);

    return this.http.get<AppNotification[]>(this.apiUrl, { params });
  }

  getUnreadCount(): Observable<{ unreadCount: number }> {
    return this.http.get<{ unreadCount: number }>(
      `${this.apiUrl}/unread-count`
    );
  }

  markAsRead(id: number): Observable<AppNotification> {
    return this.http.patch<AppNotification>(
      `${this.apiUrl}/${id}/read`,
      {}
    );
  }

  markAllAsRead(): Observable<{ updatedCount: number }> {
    return this.http.patch<{ updatedCount: number }>(
      `${this.apiUrl}/read-all`,
      {}
    );
  }
}
