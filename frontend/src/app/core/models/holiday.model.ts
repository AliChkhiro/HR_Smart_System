export type HolidayType = 'NATIONAL' | 'RELIGIOUS';

export interface HolidayDto {
  date: string;
  name: string;
  type: HolidayType;
}
