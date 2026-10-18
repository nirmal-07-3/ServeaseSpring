
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
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './sidebar.html',
  styleUrl: './sidebar.css'
})
export class Sidebar {
  @Input() isOpen = false;
  @Output() close = new EventEmitter<void>();

  navigationItems: NavigationItem[] = [];
  dashboardRoute = '/home';
  profileRoute = '/profile';
  settingsRoute = '/settings';
  userRole = '';

  constructor(
    private auth: Auth,
    private router: Router
  ) {
    this.configureNavigation();
  }

  private configureNavigation(): void {
    this.userRole = (this.auth.getRole() ?? '')
      .toUpperCase()
      .replace('ROLE_', '');

    if (this.userRole === 'ADMIN') {
      this.dashboardRoute = '/admin/dashboard';
      this.profileRoute = '/admin/profile';
      this.settingsRoute = '/admin/settings';

      this.navigationItems = [
        { label: 'Dashboard', icon: '⌂', route: '/admin/dashboard' },
        { label: 'Users', icon: '♙', route: '/admin/users' },
        { label: 'Providers', icon: '♟', route: '/admin/providers' },
        { label: 'Categories', icon: '◈', route: '/admin/categories' },
        { label: 'Profile', icon: '○', route: '/admin/profile' },
        { label: 'Platform Settings', icon: '⚙', route: '/admin/settings' }
      ];

      return;
    }

    if (this.userRole === 'PROVIDER') {
      this.dashboardRoute = '/provider/dashboard';
      this.profileRoute = '/provider/profile';

      this.navigationItems = [
        { label: 'Dashboard', icon: '⌂', route: '/provider/dashboard' },
        { label: 'My Services', icon: '◈', route: '/provider/services' },
        { label: 'Bookings', icon: '▣', route: '/provider/bookings' },
        { label: 'Availability', icon: '◷', route: '/provider/availability' },
        { label: 'Messages', icon: '◌', route: '/provider/messages' },
        { label: 'Profile', icon: '○', route: '/provider/profile' },
        { label: 'Verification', icon: '✓', route: '/provider/verification' }
      ];

      return;
    }

    this.dashboardRoute = '/home';
    this.profileRoute = '/profile';
    this.settingsRoute = '/settings';

    this.navigationItems = [
      { label: 'Dashboard', icon: '⌂', route: '/home' },
      { label: 'Services', icon: '◈', route: '/services' },
      { label: 'Bookings', icon: '▣', route: '/bookings' },
      { label: 'Messages', icon: '◌', route: '/customer/messages' },
      { label: 'Profile', icon: '○', route: '/profile' },
      { label: 'Favorites', icon: '♡', route: '/favorites' },
      { label: 'Search Services', icon: '⌕', route: '/service-search' }
    ];
  }

  closeSidebar(): void {
    this.close.emit();
  }

  logout(): void {
    this.auth.logout();
    this.closeSidebar();
    this.router.navigate(['/login']);
  }
}
