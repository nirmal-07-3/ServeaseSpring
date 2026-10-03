import {
  Component,
  OnInit,
  ChangeDetectorRef
} from '@angular/core';

import {
  CommonModule
} from '@angular/common';

import {
  ActivatedRoute,
  RouterLink
} from '@angular/router';

import {
  Api
} from '../../../core/services/api';

import {
  finalize
} from 'rxjs/operators';


interface SubCategory {

  id: number;

  categoryId: number;

  categoryName: string;

  name: string;

  description: string;

  icon: string;

  displayOrder: number;

  active: boolean;

}


interface ApiResponse<T> {

  success: boolean;

  message: string;

  data: T;

}


@Component({

  selector: 'app-category-services',

  standalone: true,

  imports: [
    CommonModule,
    RouterLink
  ],

  templateUrl: './category-services.html',

  styleUrl: './category-services.css'

})


export class CategoryServices implements OnInit {


  categoryId!: number;

  categoryName = 'Services';


  subCategories: SubCategory[] = [];


  loading = false;

  error = '';


  constructor(

    private route: ActivatedRoute,

    private api: Api,

    private cdr: ChangeDetectorRef

  ) {}


  ngOnInit(): void {

    this.route.paramMap.subscribe(params => {

      const id =
        Number(params.get('categoryId'));

      if (!id || id <= 0) {

        this.error =
          'Invalid service category.';

        return;

      }


      this.categoryId = id;

      this.loadSubCategories();

    });

  }


  loadSubCategories(): void {

    this.loading = true;

    this.error = '';

    this.subCategories = [];


    this.api

      .get<ApiResponse<SubCategory[]>>(
        `/categories/${this.categoryId}/subcategories`
      )

      .pipe(

        finalize(() => {

          this.loading = false;

          this.cdr.detectChanges();

        })

      )

      .subscribe({

        next: (response) => {

          console.log(
            'SUBCATEGORIES RESPONSE:',
            response
          );


          if (

            response &&

            response.success &&

            Array.isArray(response.data)

          ) {

            this.subCategories =
              response.data;


            if (
              this.subCategories.length > 0 &&
              this.subCategories[0].categoryName
            ) {

              this.categoryName =
                this.subCategories[0].categoryName;

            }

          }

          else {

            this.error =
              response?.message ||
              'Unable to load services.';

          }


          this.cdr.detectChanges();

        },


        error: (error) => {

          console.error(
            'SUBCATEGORY LOADING ERROR:',
            error
          );


          this.error =
            error?.error?.message ||
            'Unable to load services right now. Please try again.';


          this.cdr.detectChanges();

        }

      });

  }


  retry(): void {

    if (this.loading) {

      return;

    }

    this.loadSubCategories();

  }


  getIcon(subCategory: SubCategory): string {

    const icon = (
      subCategory.icon || ''
    ).toLowerCase().trim();

    const iconMap: Record<string, string> = {

      home: '⌂',

      electrical: 'ϟ',

      plumbing: '⌁',

      cleaning: '✦',

      ac: '❄',

      'ac-repair': '❄',

      painting: '◒',

      appliance: '▣',

      'appliance-repair': '▣',

      carpentry: '⌘',

      pest: '♧',

      moving: '⇄'

    };

    return iconMap[icon] || '✦';
  }

}
