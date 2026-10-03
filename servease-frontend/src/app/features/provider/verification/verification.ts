import {
  Component,
  OnInit,
  ChangeDetectorRef
} from '@angular/core';

import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { finalize } from 'rxjs/operators';


interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}


export type DocumentType =
  | 'AADHAR'
  | 'PAN'
  | 'GST'
  | 'SHOP_LICENSE'
  | 'OTHER';


export interface ProviderDocument {

  id: number;

  documentType: DocumentType;

  documentNumber: string;

  documentUrl: string;

  verified: boolean;

  uploadedAt: string;
}


@Component({

  selector: 'app-provider-verification',

  standalone: true,

  imports: [
    CommonModule,
    FormsModule,
    RouterLink
  ],

  templateUrl: './verification.html',

  styleUrl: './verification.css'

})


export class Verification implements OnInit {


  // ============================================================
  // DOCUMENT TYPES
  // ============================================================

  documentTypes: {
    value: DocumentType;
    label: string;
  }[] = [

    {
      value: 'AADHAR',
      label: 'Aadhaar Card'
    },

    {
      value: 'PAN',
      label: 'PAN Card'
    },

    {
      value: 'GST',
      label: 'GST Certificate'
    },

    {
      value: 'SHOP_LICENSE',
      label: 'Shop License'
    },

    {
      value: 'OTHER',
      label: 'Other Document'
    }

  ];


  // ============================================================
  // FORM
  // ============================================================

  selectedDocumentType: DocumentType = 'AADHAR';

  documentNumber = '';

  selectedFile: File | null = null;


  // ============================================================
  // EXISTING DOCUMENTS
  // ============================================================

  documents: ProviderDocument[] = [];


  // ============================================================
  // LOADING STATES
  // ============================================================

  loading = false;

  uploading = false;


  // ============================================================
  // MESSAGES
  // ============================================================

  errorMessage = '';

  successMessage = '';


  // ============================================================
  // FILE NAME
  // ============================================================

  selectedFileName = '';


  constructor(

    private http: HttpClient,

    private cdr: ChangeDetectorRef

  ) {}


  // ============================================================
  // INIT
  // ============================================================

  ngOnInit(): void {

    this.loadDocuments();

  }


  // ============================================================
  // LOAD DOCUMENTS
  // ============================================================

  loadDocuments(): void {

    this.loading = true;

    this.errorMessage = '';

    this.http

      .get<ApiResponse<ProviderDocument[]>>(
        '/api/providers/documents'
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
            'VERIFICATION: DOCUMENT RESPONSE =',
            response
          );


          if (

            response &&

            response.success &&

            Array.isArray(response.data)

          ) {

            this.documents = response.data;

          }

          else {

            this.documents = [];

            this.errorMessage =
              response?.message ||
              'Unable to load your documents.';

          }

        },


        error: (error) => {

          console.error(
            'VERIFICATION: DOCUMENT LOAD ERROR =',
            error
          );


          this.documents = [];


          this.errorMessage =
            error?.error?.message ||
            'Unable to load your verification documents.';

        }

      });

  }


  // ============================================================
  // FILE SELECT
  // ============================================================

  onFileSelected(
    event: Event
  ): void {

    this.errorMessage = '';

    this.successMessage = '';

    this.selectedFile = null;

    this.selectedFileName = '';


    const input =
      event.target as HTMLInputElement;


    if (

      !input.files ||

      input.files.length === 0

    ) {

      return;

    }


    const file =
      input.files[0];


    // ----------------------------------------------------------
    // FILE TYPE
    // ----------------------------------------------------------

    const allowedTypes = [

      'application/pdf',

      'image/jpeg',

      'image/jpg',

      'image/png'

    ];


    if (!allowedTypes.includes(file.type)) {

      this.errorMessage =
        'Only PDF, JPG, JPEG and PNG files are allowed.';

      input.value = '';

      return;

    }


    // ----------------------------------------------------------
    // FILE SIZE
    // ----------------------------------------------------------

    const maxSize =
      5 * 1024 * 1024;


    if (file.size > maxSize) {

      this.errorMessage =
        'File size must not exceed 5 MB.';

      input.value = '';

      return;

    }


    this.selectedFile = file;

    this.selectedFileName = file.name;

  }


  // ============================================================
  // DOCUMENT TYPE CHANGE
  // ============================================================

  onDocumentTypeChange(): void {

    this.errorMessage = '';

    this.successMessage = '';

  }


  // ============================================================
  // CHECK WHETHER DOCUMENT ALREADY EXISTS
  // ============================================================

  hasDocumentType(
    documentType: DocumentType
  ): boolean {

    return this.documents.some(

      document =>
        document.documentType === documentType

    );

  }


  // ============================================================
  // SELECTED DOCUMENT ALREADY EXISTS
  // ============================================================

  selectedTypeAlreadyUploaded(): boolean {

    return this.hasDocumentType(
      this.selectedDocumentType
    );

  }


  // ============================================================
  // UPLOAD DOCUMENT
  // ============================================================

  uploadDocument(): void {

    this.errorMessage = '';

    this.successMessage = '';


    // ----------------------------------------------------------
    // PREVENT MULTIPLE REQUESTS
    // ----------------------------------------------------------

    if (this.uploading) {

      return;

    }


    // ----------------------------------------------------------
    // DOCUMENT TYPE
    // ----------------------------------------------------------

    if (!this.selectedDocumentType) {

      this.errorMessage =
        'Please select a document type.';

      return;

    }


    // ----------------------------------------------------------
    // DUPLICATE CHECK
    // ----------------------------------------------------------

    if (this.selectedTypeAlreadyUploaded()) {

      this.errorMessage =
        `${this.getDocumentLabel(
          this.selectedDocumentType
        )} has already been uploaded.`;

      return;

    }


    // ----------------------------------------------------------
    // DOCUMENT NUMBER
    // ----------------------------------------------------------

    if (!this.documentNumber.trim()) {

      this.errorMessage =
        'Please enter the document number.';

      return;

    }


    // ----------------------------------------------------------
    // FILE
    // ----------------------------------------------------------

    if (!this.selectedFile) {

      this.errorMessage =
        'Please select a document file.';

      return;

    }


    // ----------------------------------------------------------
    // CREATE FORM DATA
    // ----------------------------------------------------------

    const formData =
      new FormData();


    formData.append(
      'file',
      this.selectedFile
    );


    formData.append(
      'documentType',
      this.selectedDocumentType
    );


    formData.append(
      'documentNumber',
      this.documentNumber.trim()
    );


    // ----------------------------------------------------------
    // START UPLOAD
    // ----------------------------------------------------------

    this.uploading = true;


    console.log(
      'VERIFICATION: UPLOAD START',
      {
        documentType:
        this.selectedDocumentType,

        documentNumber:
          this.documentNumber.trim(),

        file:
        this.selectedFile.name
      }
    );


    this.http

      .post<ApiResponse<ProviderDocument>>(
        '/api/providers/documents',
        formData
      )

      .pipe(

        finalize(() => {

          this.uploading = false;

          this.cdr.detectChanges();

        })

      )

      .subscribe({

        next: (response) => {

          console.log(
            'VERIFICATION: UPLOAD RESPONSE =',
            response
          );


          if (

            response &&

            response.success &&

            response.data

          ) {

            this.successMessage =
              response.message ||
              'Document uploaded successfully.';


            // --------------------------------------------------
            // RESET FORM
            // --------------------------------------------------

            this.documentNumber = '';

            this.selectedFile = null;

            this.selectedFileName = '';


            const fileInput =
              document.getElementById(
                'documentFile'
              ) as HTMLInputElement | null;


            if (fileInput) {

              fileInput.value = '';

            }


            // --------------------------------------------------
            // RELOAD DOCUMENTS
            // --------------------------------------------------

            this.loadDocuments();

          }

          else {

            this.errorMessage =
              response?.message ||
              'Unable to upload the document.';

          }

        },


        error: (error) => {

          console.error(
            'VERIFICATION: UPLOAD ERROR =',
            error
          );


          this.errorMessage =
            error?.error?.message ||
            'Unable to upload the document.';

        }

      });

  }


  // ============================================================
  // DOCUMENT LABEL
  // ============================================================

  getDocumentLabel(
    type: DocumentType
  ): string {

    const document =
      this.documentTypes.find(
        item => item.value === type
      );


    return document?.label || type;

  }


  // ============================================================
  // DOCUMENT STATUS
  // ============================================================

  getDocumentStatus(
    document: ProviderDocument
  ): string {

    if (document.verified) {

      return 'Verified';

    }


    return 'Pending review';

  }


  // ============================================================
  // DOCUMENT STATUS CLASS
  // ============================================================

  getDocumentStatusClass(
    document: ProviderDocument
  ): string {

    if (document.verified) {

      return 'verified';

    }


    return 'pending';

  }


  // ============================================================
  // MASK DOCUMENT NUMBER
  // ============================================================

  maskDocumentNumber(
    documentNumber: string
  ): string {

    if (!documentNumber) {

      return '-';

    }


    const value =
      documentNumber.trim();


    if (value.length <= 4) {

      return value;

    }


    const visible =
      value.slice(-4);


    return `${'•'.repeat(
      Math.min(
        value.length - 4,
        8
      )
    )}${visible}`;

  }


  // ============================================================
  // FORMAT DATE
  // ============================================================

  formatDate(
    date: string
  ): string {

    if (!date) {

      return '-';

    }


    const parsed =
      new Date(date);


    if (Number.isNaN(parsed.getTime())) {

      return date;

    }


    return parsed.toLocaleDateString(
      'en-IN',
      {
        day: '2-digit',
        month: 'short',
        year: 'numeric'
      }
    );

  }


  // ============================================================
  // FILE SIZE
  // ============================================================

  formatFileSize(
    bytes: number
  ): string {

    if (!bytes) {

      return '0 KB';

    }


    if (bytes < 1024) {

      return `${bytes} B`;

    }


    const kb =
      bytes / 1024;


    if (kb < 1024) {

      return `${kb.toFixed(1)} KB`;

    }


    const mb =
      kb / 1024;


    return `${mb.toFixed(1)} MB`;

  }


  // ============================================================
  // VIEW DOCUMENT
  // ============================================================

  viewDocument(
    document: ProviderDocument
  ): void {

    if (!document.documentUrl) {

      this.errorMessage =
        'Document URL is not available.';

      return;

    }


    let url =
      document.documentUrl;


    // Backend currently returns paths such as:
    //
    // providers/documents/...
    //
    // Convert relative storage path into
    // the backend URL.

    if (
      !url.startsWith('http://') &&
      !url.startsWith('https://')
    ) {

      url =
        `http://localhost:8080/${url}`;

    }


    window.open(
      url,
      '_blank',
      'noopener,noreferrer'
    );

  }


  // ============================================================
  // REFRESH
  // ============================================================

  refreshDocuments(): void {

    if (this.loading || this.uploading) {

      return;

    }


    this.successMessage = '';

    this.loadDocuments();

  }


  // ============================================================
  // DOCUMENT COUNTS
  // ============================================================

  get verifiedCount(): number {

    return this.documents.filter(
      document => document.verified
    ).length;

  }


  get pendingCount(): number {

    return this.documents.filter(
      document => !document.verified
    ).length;

  }

}
