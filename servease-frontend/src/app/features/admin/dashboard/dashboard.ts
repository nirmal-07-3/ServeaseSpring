import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Auth } from '../../../core/services/auth';

interface Stat {
  label: string;
  value: string;
  change: string;
  icon: string;
  positive: boolean;
}

interface Activity {
  title: string;
  description: string;
  time: string;
  icon: string;
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink
  ],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css'
})
export class Dashboard {

  adminName = 'Admin';

  stats: Stat[] = [
    {
      label: 'Total Users',
      value: '1,248',
      change: '+12.5%',
      icon: '◉',
      positive: true
    },
    {
      label: 'Providers',
      value: '186',
      change: '+8.2%',
      icon: '♙',
      positive: true
    },
    {
      label: 'Total Bookings',
      value: '3,642',
      change: '+18.4%',
      icon: '▣',
      positive: true
    },
    {
      label: 'Revenue',
      value: '₹8.42L',
      change: '+14.7%',
      icon: '₹',
      positive: true
    }
  ];

  activities: Activity[] = [
    {
      title: 'New provider registration',
      description: 'A new provider is waiting for verification.',
      time: '12 min ago',
      icon: '♙'
    },
    {
      title: 'New booking received',
      description: 'A customer booked a home cleaning service.',
      time: '28 min ago',
      icon: '▣'
    },
    {
      title: 'Provider profile updated',
      description: 'Provider information has been updated.',
      time: '1 hour ago',
      icon: '✎'
    },
    {
      title: 'New customer registered',
      description: 'A new customer joined Servease.',
      time: '2 hours ago',
      icon: '◉'
    }
  ];

  constructor(
    private auth: Auth
  ) {
    const user = this.auth.getUser();

    if (user?.name) {
      this.adminName = user.name;
    }
  }

  getAdminInitials(): string {
    if (!this.adminName || this.adminName.trim() === '') {
      return 'A';
    }

    const parts = this.adminName.trim().split(/\s+/);

    if (parts.length === 1) {
      return parts[0].charAt(0).toUpperCase();
    }

    return (
      parts[0].charAt(0) +
      parts[parts.length - 1].charAt(0)
    ).toUpperCase();
  }
}
