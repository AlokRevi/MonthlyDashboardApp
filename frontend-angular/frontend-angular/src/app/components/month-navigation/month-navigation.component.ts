import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-month-navigation',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './month-navigation.component.html',
  styleUrl: './month-navigation.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class MonthNavigationComponent {
  @Input() selectedYear = new Date().getFullYear();
  @Input() selectedMonth = new Date().getMonth() + 1;
  @Input() loading = false;
  @Input() monthOptions: { value: number; label: string }[] = [];

  @Output() selectedYearChange = new EventEmitter<number>();
  @Output() selectedMonthChange = new EventEmitter<number>();
  @Output() loadMonth = new EventEmitter<void>();
  @Output() previousMonth = new EventEmitter<void>();
  @Output() currentMonth = new EventEmitter<void>();
  @Output() nextMonth = new EventEmitter<void>();

  onYearChanged(value: number): void {
    this.selectedYear = Number(value);
    this.selectedYearChange.emit(this.selectedYear);
  }

  onMonthChanged(value: number): void {
    this.selectedMonth = Number(value);
    this.selectedMonthChange.emit(this.selectedMonth);
  }
}
