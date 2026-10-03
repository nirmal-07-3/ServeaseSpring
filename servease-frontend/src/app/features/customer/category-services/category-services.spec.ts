import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CategoryServices } from './category-services';

describe('CategoryServices', () => {
  let component: CategoryServices;
  let fixture: ComponentFixture<CategoryServices>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CategoryServices],
    }).compileComponents();

    fixture = TestBed.createComponent(CategoryServices);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
