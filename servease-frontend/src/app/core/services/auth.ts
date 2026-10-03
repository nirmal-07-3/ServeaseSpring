import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';


/* ================================
   Login Request
================================ */

export interface LoginRequest {
  email: string;
  password: string;
}


/* ================================
   Authentication User
================================ */

export interface AuthUser {
  id: number;
  name: string;
  email: string;
  role: string;
  token: string;
}


/* ================================
   Backend API Response
================================ */

export interface AuthResponse {
  success: boolean;
  message: string;
  data: AuthUser;
}


@Injectable({
  providedIn: 'root'
})
export class Auth {

  private readonly TOKEN_KEY = 'servease_token';
  private readonly USER_KEY = 'servease_user';

  constructor(
    private http: HttpClient
  ) {}


  /* ================================
     LOGIN
  ================================ */

  login(
    request: LoginRequest
  ): Observable<AuthResponse> {

    return this.http.post<AuthResponse>(
      '/api/auth/login',
      request
    );
  }


  /* ================================
     SAVE AUTHENTICATION
  ================================ */

  saveAuthentication(
    user: AuthUser
  ): void {

    localStorage.setItem(
      this.TOKEN_KEY,
      user.token
    );

    localStorage.setItem(
      this.USER_KEY,
      JSON.stringify(user)
    );
  }


  /* ================================
     GET JWT TOKEN
  ================================ */

  getToken(): string | null {

    return localStorage.getItem(
      this.TOKEN_KEY
    );
  }


  /* ================================
     GET LOGGED-IN USER
  ================================ */

  getUser(): AuthUser | null {

    const user =
      localStorage.getItem(
        this.USER_KEY
      );

    if (!user) {
      return null;
    }

    try {

      return JSON.parse(
        user
      ) as AuthUser;

    } catch {

      return null;
    }
  }


  /* ================================
     CHECK LOGIN STATUS
  ================================ */

  isLoggedIn(): boolean {

    return !!this.getToken();
  }


  /* ================================
     GET USER ROLE
  ================================ */

  getRole(): string | null {

    return this.getUser()?.role ?? null;
  }


  /* ================================
     UPDATE LOGGED-IN USER NAME
  ================================ */

  updateUserName(
    name: string
  ): void {

    const user = this.getUser();

    if (!user) {
      return;
    }

    const updatedUser: AuthUser = {

      ...user,

      name: name

    };

    localStorage.setItem(
      this.USER_KEY,
      JSON.stringify(updatedUser)
    );
  }


  /* ================================
     LOGOUT
  ================================ */

  logout(): void {

    localStorage.removeItem(
      this.TOKEN_KEY
    );

    localStorage.removeItem(
      this.USER_KEY
    );
  }
}
