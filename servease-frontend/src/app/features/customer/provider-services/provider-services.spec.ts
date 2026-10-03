import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ProviderServices } from './provider-services';

describe('ProviderServices', () => {
  let component: ProviderServices;
  let fixture: ComponentFixture<ProviderServices>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProviderServices],
    }).compileComponents();

    fixture = TestBed.createComponent(ProviderServices);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
