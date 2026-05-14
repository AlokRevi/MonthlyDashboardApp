import { DOCUMENT } from '@angular/common';
import { Inject, Injectable, signal } from '@angular/core';

export const SUPPORTED_THEME_IDS = [
  'theme-default',
  'theme-solar-punk',
  'theme-cyberpunk',
  'theme-space-sci-fi',
  'theme-matrix-coder'
] as const;

export type ThemeId = typeof SUPPORTED_THEME_IDS[number];

const DEFAULT_THEME_ID: ThemeId = 'theme-default';
const THEME_STORAGE_KEY = 'monthly-dashboard-theme';

@Injectable({
  providedIn: 'root'
})
export class ThemeService {
  readonly activeTheme = signal<ThemeId>(DEFAULT_THEME_ID);

  constructor(@Inject(DOCUMENT) private document: Document) {}

  initializeTheme(): void {
    this.setTheme(this.getStoredTheme());
  }

  setTheme(themeId: ThemeId): void {
    const safeThemeId = this.isSupportedTheme(themeId) ? themeId : DEFAULT_THEME_ID;

    this.activeTheme.set(safeThemeId);
    this.applyThemeClass(safeThemeId);
    this.document.defaultView?.localStorage.setItem(THEME_STORAGE_KEY, safeThemeId);
  }

  private getStoredTheme(): ThemeId {
    const storedTheme = this.document.defaultView?.localStorage.getItem(THEME_STORAGE_KEY);

    return this.isSupportedTheme(storedTheme) ? storedTheme : DEFAULT_THEME_ID;
  }

  private applyThemeClass(themeId: ThemeId): void {
    const root = this.document.documentElement;

    root.classList.remove(...SUPPORTED_THEME_IDS);
    root.classList.add(themeId);
  }

  private isSupportedTheme(themeId: unknown): themeId is ThemeId {
    return typeof themeId === 'string'
      && SUPPORTED_THEME_IDS.includes(themeId as ThemeId);
  }
}
