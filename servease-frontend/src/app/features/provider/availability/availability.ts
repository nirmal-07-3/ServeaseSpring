import {
  Component,
  OnInit,
  ChangeDetectorRef
} from '@angular/core';

import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { finalize } from 'rxjs';


interface ProviderAvailability {

  id?: number;

  dayOfWeek: string;

  available: boolean;

  startTime: string | null;

  endTime: string | null;
}


interface ApiResponse<T> {

  success: boolean;

  message: string;

  data: T;
}


@Component({

  selector: 'app-availability',

  standalone: true,

  imports: [
    CommonModule,
    FormsModule,
    RouterLink
  ],

  templateUrl: './availability.html',

  styleUrl: './availability.css'

})


export class Availability implements OnInit {


  loading = true;

  saving = false;


  errorMessage = '';

  successMessage = '';


  availability: ProviderAvailability[] = [

    {
      dayOfWeek: 'MONDAY',
      available: false,
      startTime: null,
      endTime: null
    },

    {
      dayOfWeek: 'TUESDAY',
      available: false,
      startTime: null,
      endTime: null
    },

    {
      dayOfWeek: 'WEDNESDAY',
      available: false,
      startTime: null,
      endTime: null
    },

    {
      dayOfWeek: 'THURSDAY',
      available: false,
      startTime: null,
      endTime: null
    },

    {
      dayOfWeek: 'FRIDAY',
      available: false,
      startTime: null,
      endTime: null
    },

    {
      dayOfWeek: 'SATURDAY',
      available: false,
      startTime: null,
      endTime: null
    },

    {
      dayOfWeek: 'SUNDAY',
      available: false,
      startTime: null,
      endTime: null
    }

  ];


  constructor(

    private http: HttpClient,

    private cdr: ChangeDetectorRef

  ) {}


  ngOnInit(): void {

    console.log(
      'AVAILABILITY: component initialized'
    );

    this.loadAvailability();

  }


  // ============================================================
  // LOAD AVAILABILITY
  // ============================================================

  loadAvailability(): void {

    this.loading = true;

    this.errorMessage = '';

    this.successMessage = '';

    console.log(
      'AVAILABILITY: loading started'
    );


    this.http
      .get<ApiResponse<ProviderAvailability[]>>(
        '/api/providers/availability'
      )
      .pipe(

        finalize(() => {

          this.loading = false;

          console.log(
            'AVAILABILITY: loading finalized =',
            this.loading
          );

          this.cdr.detectChanges();

        })

      )
      .subscribe({

        next: (response) => {

          console.log(
            'AVAILABILITY: API RESPONSE',
            response
          );


          if (
            response &&
            response.success &&
            Array.isArray(response.data)
          ) {

            /*
             * Always keep all 7 days visible.
             *
             * Backend may return only saved days,
             * so merge saved values into the
             * existing seven-day structure.
             */

            this.availability =
              this.availability.map(defaultDay => {

                const savedDay =
                  response.data.find(
                    day =>
                      day.dayOfWeek ===
                      defaultDay.dayOfWeek
                  );


                if (savedDay) {

                  return {
                    ...defaultDay,
                    ...savedDay
                  };

                }


                return defaultDay;

              });

          }


          else {

            this.errorMessage =
              response?.message ||
              'Unable to load availability.';

          }


          console.log(
            'AVAILABILITY: final data',
            this.availability
          );

        },


        error: (error) => {

          console.error(
            'AVAILABILITY: API ERROR',
            error
          );


          this.errorMessage =
            error?.error?.message ||
            'Unable to load your availability.';

        }

      });

  }


  // ============================================================
  // SAVE AVAILABILITY
  // ============================================================

  saveAvailability(): void {

    this.errorMessage = '';

    this.successMessage = '';


    /*
     * Validate every available day.
     */

    for (const day of this.availability) {

      if (day.available) {


        if (
          !day.startTime ||
          !day.endTime
        ) {

          this.errorMessage =
            `Please select start and end time for ${this.getDayLabel(day.dayOfWeek)}.`;

          return;

        }


        if (
          day.startTime >=
          day.endTime
        ) {

          this.errorMessage =
            `End time must be after start time for ${this.getDayLabel(day.dayOfWeek)}.`;

          return;

        }

      }


      else {

        /*
         * Unavailable days must
         * have null times.
         */

        day.startTime = null;

        day.endTime = null;

      }

    }


    this.saving = true;


    const request: ProviderAvailability[] =
      this.availability.map(day => ({

        id: day.id,

        dayOfWeek: day.dayOfWeek,

        available: day.available,

        startTime: day.startTime,

        endTime: day.endTime

      }));


    console.log(
      'AVAILABILITY: saving',
      request
    );


    this.http
      .put<ApiResponse<null>>(
        '/api/providers/availability',
        request
      )
      .pipe(

        finalize(() => {

          this.saving = false;

          this.cdr.detectChanges();

        })

      )
      .subscribe({

        next: (response) => {

          console.log(
            'AVAILABILITY: SAVE RESPONSE',
            response
          );


          if (
            response &&
            response.success
          ) {

            this.successMessage =
              response.message ||
              'Availability updated successfully.';

          }


          else {

            this.errorMessage =
              response?.message ||
              'Unable to update availability.';

          }

        },


        error: (error) => {

          console.error(
            'AVAILABILITY: SAVE ERROR',
            error
          );


          this.errorMessage =
            error?.error?.message ||
            'Unable to update your availability.';

        }

      });

  }


  // ============================================================
  // HELPERS
  // ============================================================

  getDayLabel(day: string): string {

    const labels: Record<string, string> = {

      MONDAY: 'Monday',

      TUESDAY: 'Tuesday',

      WEDNESDAY: 'Wednesday',

      THURSDAY: 'Thursday',

      FRIDAY: 'Friday',

      SATURDAY: 'Saturday',

      SUNDAY: 'Sunday'

    };


    return labels[day] || day;

  }


  formatDay(day: string): string {

    return this.getDayLabel(day);

  }


  get availableDays(): number {

    return this.availability.filter(
      day => day.available
    ).length;

  }


  get unavailableDays(): number {

    return this.availability.filter(
      day => !day.available
    ).length;

  }

}
