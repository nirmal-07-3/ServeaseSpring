import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ProviderCard } from './provider-card';

describe('ProviderCard', () => {
  let component: ProviderCard;
  let fixture: ComponentFixture<ProviderCard>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProviderCard],
    }).compileComponents();

    fixture = TestBed.createComponent(ProviderCard);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
