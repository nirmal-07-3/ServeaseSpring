import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CreateService } from './create-service';

describe('CreateService', () => {
  let component: CreateService;
  let fixture: ComponentFixture<CreateService>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CreateService],
    }).compileComponents();

    fixture = TestBed.createComponent(CreateService);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
