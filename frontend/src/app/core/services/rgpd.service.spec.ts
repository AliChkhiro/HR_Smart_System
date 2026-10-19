import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { RgpdService } from './rgpd.service';

describe('RgpdService', () => {
  let service: RgpdService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [RgpdService, provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(RgpdService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
  });

  it('exports the personal data of the current user', () => {
    service.exportPersonalData().subscribe();
    const req = http.expectOne(request => request.url.endsWith('/rgpd/export') && request.method === 'GET');
    expect(req.request.method).toBe('GET');
    req.flush({ generatedAt: '2026-01-01T00:00:00Z', account: {}, employee: null, skills: [], leaves: [], chatMessages: [] });
  });

  it('posts an erasure request with a trimmed reason', () => {
    service.requestErasure('  je pars  ').subscribe();
    const req = http.expectOne(request => request.url.endsWith('/rgpd/erasure-request') && request.method === 'POST');
    expect(req.request.body).toEqual({ reason: 'je pars' });
    req.flush({ id: 1, status: 'PENDING' });
  });

  it('sends a null reason when the field is blank', () => {
    service.requestErasure('   ').subscribe();
    const req = http.expectOne(request => request.url.endsWith('/rgpd/erasure-request'));
    expect(req.request.body).toEqual({ reason: null });
    req.flush({ id: 1, status: 'PENDING' });
  });

  it('loads the erasure requests of the current user', () => {
    service.myErasureRequests().subscribe();
    const req = http.expectOne(request => request.url.endsWith('/rgpd/my-erasure-requests') && request.method === 'GET');
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('filters the admin erasure list by status', () => {
    service.listErasureRequests('PENDING').subscribe();
    const req = http.expectOne(request => request.url.endsWith('/rgpd/erasure-requests') && request.method === 'GET');
    expect(req.request.params.get('status')).toBe('PENDING');
    req.flush({ content: [], totalElements: 0, totalPages: 0, size: 50, number: 0 });
  });

  it('approves an erasure request', () => {
    service.approveErasure(7).subscribe();
    const req = http.expectOne(request => request.url.endsWith('/rgpd/erasure-requests/7/approve') && request.method === 'POST');
    expect(req.request.method).toBe('POST');
    req.flush({ id: 7, status: 'APPROVED' });
  });

  it('rejects an erasure request with a comment', () => {
    service.rejectErasure(9, 'motif').subscribe();
    const req = http.expectOne(request => request.url.endsWith('/rgpd/erasure-requests/9/reject') && request.method === 'POST');
    expect(req.request.body).toEqual({ comment: 'motif' });
    req.flush({ id: 9, status: 'REJECTED' });
  });

  it('loads the audit log with pagination', () => {
    service.auditLog(1, 25).subscribe();
    const req = http.expectOne(request => request.url.endsWith('/audit') && request.method === 'GET');
    expect(req.request.params.get('page')).toBe('1');
    expect(req.request.params.get('size')).toBe('25');
    req.flush({ content: [], totalElements: 0, totalPages: 0, size: 25, number: 1 });
  });
});
