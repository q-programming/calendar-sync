import { describe, expect, it } from 'vitest';
import { screen } from '@testing-library/react';
import { render } from 'vitest-browser-react';
import NotFound from './not-found';

describe('NotFound', () => {
    it('should render the 404 heading', () => {
        render(<NotFound />);
        expect(screen.getByText('404 Page Not Found')).toBeInTheDocument();
    });

    it('should render the helper text', () => {
        render(<NotFound />);
        expect(screen.getByText(/did you forget to add the page/i)).toBeInTheDocument();
    });

    it('should render an error icon', () => {
        const { container } = render(<NotFound />);
        // MUI ErrorIcon renders an SVG
        expect(container.querySelector('svg')).toBeInTheDocument();
    });
});
