import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { Sidebar } from '../../components/sidebar/sidebar';

@Component({
  selector: 'app-app-layout',
  standalone: true,
  imports: [
    RouterOutlet,
    Sidebar
  ],
  templateUrl: './app-layout.html',
  styleUrl: './app-layout.css'
})
export class AppLayout {

  sidebarOpen = false;

  toggleSidebar(): void {
    this.sidebarOpen = !this.sidebarOpen;
  }

  closeSidebar(): void {
    this.sidebarOpen = false;
  }
}
