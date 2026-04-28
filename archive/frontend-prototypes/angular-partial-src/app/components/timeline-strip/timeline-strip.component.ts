import {
  Component,
  Input,
  OnChanges,
  SimpleChanges
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { DayStripItem, ScaleBar } from '../../models/dashboard.models';

@Component({
  selector: 'app-timeline-strip',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './timeline-strip.component.html',
  styleUrl: './timeline-strip.component.css'
})
export class TimelineStripComponent implements OnChanges {
  @Input({ required: true }) scaleBar!: ScaleBar;
  @Input({ required: true }) dayStrip!: DayStripItem[];

  todayIndex = -1;
  todayLeftPercent = 0;
  todayLabel = 'Today';

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['dayStrip']) {
      this.computeTodayMarker();
    }
  }

  trackByDate(_: number, day: DayStripItem): string {
    return day.date;
  }

  trackByAnchor(_: number, anchor: number): number {
    return anchor;
  }

  getAnchorLeftPercent(anchor: number): number {
    return ((anchor - 1) / Math.max(this.scaleBar.lastDay - 1, 1)) * 100;
  }

  private computeTodayMarker(): void {
    this.todayIndex = this.dayStrip.findIndex(day => day.isToday);

    if (this.todayIndex < 0) {
      this.todayLeftPercent = 0;
      this.todayLabel = 'Today';
      return;
    }

    this.todayLeftPercent =
      (this.todayIndex / Math.max(this.dayStrip.length - 1, 1)) * 100;

    const today = this.dayStrip[this.todayIndex];
    const date = new Date(today.date + 'T00:00:00');
    const month = date.toLocaleString('en-US', { month: 'short' });
    this.todayLabel = `${month} ${date.getDate()}, ${date.getFullYear()}`;
  }
}
import { ChangeDetectionStrategy, Component, Input } from '@angular/core';

@Component({
  selector: 'app-timeline-strip',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './timeline-strip.component.html',
  styleUrl: './timeline-strip.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})