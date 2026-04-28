import {
  Component,
  Input,
  OnChanges,
  SimpleChanges
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { DashboardCategory, Occurrence } from '../../models/dashboard.models';

interface TaskCellViewModel {
  day: number;
  visible: boolean;
  completed: boolean;
}

interface TaskRowViewModel {
  taskId: number;
  taskName: string;
  cells: TaskCellViewModel[];
}

@Component({
  selector: 'app-category-section',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './category-section.component.html',
  styleUrl: './category-section.component.css'
})
export class CategorySectionComponent implements OnChanges {
  @Input({ required: true }) category!: DashboardCategory;
  @Input({ required: true }) totalDays!: number;

  taskRows: TaskRowViewModel[] = [];

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['category'] || changes['totalDays']) {
      this.taskRows = this.buildTaskRows();
    }
  }

  trackByTaskId(_: number, row: TaskRowViewModel): number {
    return row.taskId;
  }

  trackByDay(_: number, cell: TaskCellViewModel): number {
    return cell.day;
  }

  private buildTaskRows(): TaskRowViewModel[] {
    if (!this.category || !this.totalDays) return [];

    return this.category.tasks.map(task => {
      const occurrenceMap = new Map<number, Occurrence>();
      task.occurrences.forEach(occ => occurrenceMap.set(occ.dayOfMonth, occ));

      const cells: TaskCellViewModel[] = [];
      for (let day = 1; day <= this.totalDays; day++) {
        const occurrence = occurrenceMap.get(day);
        cells.push({
          day,
          visible: !!occurrence,
          completed: !!occurrence?.completed
        });
      }

      return {
        taskId: task.taskId,
        taskName: task.taskName,
        cells
      };
    });
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