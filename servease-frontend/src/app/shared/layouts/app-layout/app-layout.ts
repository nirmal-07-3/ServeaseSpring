import { Component } from '@angular/core';
import { RouterLink, RouterOutlet } from '@angular/router';

import { Sidebar } from '../../components/sidebar/sidebar';
import { Auth } from '../../../core/services/auth';

@Component({
  selector: 'app-app-layout',
  standalone: true,
  imports: [
    RouterOutlet,
    RouterLink,
    Sidebar
  ],
  templateUrl: './app-layout.html',
  styleUrl: './app-layout.css'
})
export class AppLayout {

  sidebarOpen = false;

  userName = 'User';

  constructor(
    private auth: Auth
  ) {
    const user = this.auth.getUser();

    if (user?.name) {
      this.userName = user.name;
    }
  }

  toggleSidebar(): void {
    this.sidebarOpen = !this.sidebarOpen;
  }

  closeSidebar(): void {
    this.sidebarOpen = false;
  }

  getUserInitials(): string {
    if (!this.userName || this.userName.trim() === '') {
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
