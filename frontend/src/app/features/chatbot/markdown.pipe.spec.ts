import { MarkdownPipe } from './markdown.pipe';

describe('MarkdownPipe', () => {
  const pipe = new MarkdownPipe();

  it('returns an empty string for empty input', () => {
    expect(pipe.transform('')).toBe('');
    expect(pipe.transform(null)).toBe('');
    expect(pipe.transform(undefined)).toBe('');
  });

  it('escapes raw HTML (no injection)', () => {
    expect(pipe.transform('<script>alert(1)</script>'))
      .toBe('<p>&lt;script&gt;alert(1)&lt;/script&gt;</p>');
  });

  it('renders bold and inline code', () => {
    expect(pipe.transform('**gras** et `code`'))
      .toBe('<p><strong>gras</strong> et <code>code</code></p>');
  });

  it('renders italic', () => {
    expect(pipe.transform('du *texte*')).toBe('<p>du <em>texte</em></p>');
  });

  it('renders bullet lists', () => {
    expect(pipe.transform('- a\n- b')).toBe('<ul><li>a</li><li>b</li></ul>');
  });

  it('renders ordered lists', () => {
    expect(pipe.transform('1. un\n2. deux')).toBe('<ol><li>un</li><li>deux</li></ol>');
  });

  it('separates paragraphs on blank lines', () => {
    expect(pipe.transform('un\n\ndeux')).toBe('<p>un</p><p>deux</p>');
  });

  it('returns empty for nullish input', () => {
    expect(new MarkdownPipe().transform(null)).toBe('');
  });
});
