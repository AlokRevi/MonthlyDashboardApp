import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  DayStripItem,
  ScaleBar
} from '../../models/dashboard.models';

@Component({
  selector: 'app-timeline-strip',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './timeline-strip.component.html',
  styleUrl: './timeline-strip.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class TimelineStripComponent {
  @Input() dayStrip: DayStripItem[] = [];
  @Input() scaleBar: ScaleBar | null = null;
}
