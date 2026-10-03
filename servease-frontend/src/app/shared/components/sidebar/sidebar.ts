import { Component, EventEmitter, Input, Output } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { Auth } from '../../../core/services/auth';

interface NavigationItem {
  label: string;
  icon: string;
  route: string;
}

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [
    RouterLink,
    RouterLinkActive
  ],
  templateUrl: './sidebar.html',
  styleUrl: './sidebar.css'
})
export class Sidebar {

  @Input() isOpen = false;

  @Output() close = new EventEmitter<void>();

  navigationItems: NavigationItem[] = [
    {
      label: 'Dashboard',
      icon: '⌂',
      route: '/home'
    },
    {
      label: 'Services',
      icon: '◈',
      route: '/services'
    },
    {
      label: 'Bookings',
      icon: '▣',
      route: '/bookings'
    },
    {
      label: 'Messages',
      icon: '◌',
      route: '/messages'
    },
    {
      label: 'Profile',
      icon: '○',
      route: '/profile'
    }
  ];

  constructor(
    private auth: Auth,
    private router: Router
  ) {}

  closeSidebar(): void {
    this.close.emit();
  }

  logout(): void {

    this.auth.logout();

    this.closeSidebar();

    this.router.navigate(['/login']);
  }
}
