import { Pipe, PipeTransform } from '@angular/core';

/**
 * Rendu Markdown minimal et sûr pour les réponses de l'assistant.
 * Le HTML est d'abord échappé, puis seuls quelques éléments contrôlés
 * (paragraphes, listes, gras, italique, code) sont générés. Angular
 * sanitise de toute façon la liaison [innerHTML].
 */
@Pipe({ name: 'markdown' })
export class MarkdownPipe implements PipeTransform {
  transform(value: string | null | undefined): string {
    if (!value) {
      return '';
    }
    const lines = this.escape(value).split('\n');
    const html: string[] = [];
    let listType: 'ul' | 'ol' | null = null;

    const closeList = () => {
      if (listType) {
        html.push(`</${listType}>`);
        listType = null;
      }
    };

    for (const raw of lines) {
      const line = raw.trimEnd();
      if (!line.trim()) {
        closeList();
        continue;
      }
      const bullet = line.match(/^\s*[-*•]\s+(.*)$/);
      const ordered = line.match(/^\s*\d+[.)]\s+(.*)$/);
      if (bullet) {
        if (listType !== 'ul') {
          closeList();
          html.push('<ul>');
          listType = 'ul';
        }
        html.push(`<li>${this.inline(bullet[1])}</li>`);
      } else if (ordered) {
        if (listType !== 'ol') {
          closeList();
          html.push('<ol>');
          listType = 'ol';
        }
        html.push(`<li>${this.inline(ordered[1])}</li>`);
      } else {
        closeList();
        html.push(`<p>${this.inline(line)}</p>`);
      }
    }
    closeList();
    return html.join('');
  }

  private inline(text: string): string {
    return text
      .replace(/`([^`]+)`/g, '<code>$1</code>')
      .replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')
      .replace(/(^|[^*])\*([^*\s][^*]*)\*/g, '$1<em>$2</em>')
      .replace(/(^|[^_\w])_([^_\s][^_]*)_/g, '$1<em>$2</em>');
  }

  private escape(value: string): string {
    return value
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;');
  }
}
