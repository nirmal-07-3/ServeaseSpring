
import { CommonModule } from '@angular/common';
import { HttpClient, HttpParams } from '@angular/common/http';
import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';

type UserRole = 'ADMIN' | 'PROVIDER' | 'CUSTOMER';
type UserStatus = 'ACTIVE' | 'PENDING' | 'BLOCKED';

interface AdminUser {
  id: number;
  name: string;
  email: string;
  phone: string;
  role: UserRole;
  status: UserStatus;
  emailVerified: boolean | null;
  phoneVerified: boolean | null;
  createdAt: string | null;
}

interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}

@Component({
  selector: 'app-admin-users',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './users.html',
  styleUrl: './users.css'
})
export class Users implements OnInit {
  users: AdminUser[] = [];

  search = '';
  selectedRole = '';
  selectedStatus = '';

  loading = false;
  updatingUserId: number | null = null;

  errorMessage = '';
  successMessage = '';

  constructor(
    private http: HttpClient,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadUsers();
  }

  loadUsers(): void {
    this.loading = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.cdr.detectChanges();

    let params = new HttpParams();

    if (this.search.trim()) {
      params = params.set('search', this.search.trim());
    }

    if (this.selectedRole) {
      params = params.set('role', this.selectedRole);
    }

    if (this.selectedStatus) {
      params = params.set('status', this.selectedStatus);
    }

    this.http
      .get<ApiResponse<AdminUser[]>>('/api/admin/users', { params })
      .pipe(
        finalize(() => {
          this.loading = false;
          this.cdr.detectChanges();
        })
      )
      .subscribe({
        next: response => {
          this.users = response.data ?? [];
          this.cdr.detectChanges();
        },
        error: error => {
          this.errorMessage =
            error?.error?.message ??
            'Unable to load users. Please try again.';

          this.cdr.detectChanges();
        }
      });
  }

  updateStatus(user: AdminUser): void {
    const newStatus: UserStatus =
      user.status === 'ACTIVE' ? 'BLOCKED' : 'ACTIVE';

    const action = newStatus === 'BLOCKED' ? 'block' : 'activate';

    if (!window.confirm(`Are you sure you want to ${action} ${user.name}?`)) {
      return;
    }

    this.updatingUserId = user.id;
    this.errorMessage = '';
    this.successMessage = '';

    this.cdr.detectChanges();

    this.http
      .patch<ApiResponse<AdminUser>>(
        `/api/admin/users/${user.id}/status`,
{ status: newStatus }
)
.pipe(
  finalize(() => {
    this.updatingUserId = null;
    this.cdr.detectChanges();
  })
)
  .subscribe({
    next: response => {
      if (response.data) {
        this.users = this.users.map(existing =>
          existing.id === user.id ? response.data : existing
        );
      }

      this.successMessage =
        response.message || 'User status updated successfully.';

      this.cdr.detectChanges();
    },
    error: error => {
      this.errorMessage =
        error?.error?.message ??
        `Unable to ${action} this user. Please try again.`;

      this.cdr.detectChanges();
    }
  });
}

get activeCount(): number {
  return this.users.filter(user => user.status === 'ACTIVE').length;
}

get blockedCount(): number {
  return this.users.filter(user => user.status === 'BLOCKED').length;
}

get pendingCount(): number {
  return this.users.filter(user => user.status === 'PENDING').length;
}

getRoleClass(role: string): string {
  return role.toLowerCase();
}

getStatusClass(status: string): string {
  return status.toLowerCase();
}

trackByUserId(index: number, user: AdminUser): number {
  return user.id;
}
}
