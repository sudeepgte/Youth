import os
from fpdf import FPDF

class BrandGuidelinesPDF(FPDF):
    def header(self):
        self.set_font('helvetica', 'B', 20)
        self.set_text_color(15, 23, 42) # #0F172A
        self.cell(0, 15, 'Youth (Zentrix) - Brand Guidelines', 0, 1, 'C')
        self.ln(10)
        
    def footer(self):
        self.set_y(-15)
        self.set_font('helvetica', 'I', 8)
        self.set_text_color(100, 116, 139)
        self.cell(0, 10, f'Page {self.page_no()}', 0, 0, 'C')

def create_pdf():
    pdf = BrandGuidelinesPDF()
    pdf.add_page()
    
    # 1. Core Theme Explanation
    pdf.set_font('helvetica', 'B', 16)
    pdf.set_text_color(15, 23, 42)
    pdf.cell(0, 10, '1. Core Theme & Concept', 0, 1)
    
    pdf.set_font('helvetica', '', 12)
    pdf.set_text_color(51, 65, 85) # #334155
    text = "The project uses a 'Cool Blue / Ice Blue / Cyan Glow' aesthetic. It features a modern, glass-like (glassmorphism) design with floating elements, glowing text effects, and soft shadows to create a premium, 3D floating feel."
    pdf.multi_cell(0, 8, text)
    pdf.ln(10)
    
    # 2. Typography
    pdf.set_font('helvetica', 'B', 16)
    pdf.set_text_color(15, 23, 42)
    pdf.cell(0, 10, '2. Typography', 0, 1)
    
    pdf.set_font('helvetica', '', 12)
    pdf.set_text_color(51, 65, 85)
    pdf.cell(0, 8, "- Primary Font Family: 'Outfit', sans-serif", 0, 1)
    pdf.cell(0, 8, "- Text Styling: Modern, legible with support for cyan glowing effects.", 0, 1)
    pdf.ln(10)
    
    # 3. Color Palette
    pdf.set_font('helvetica', 'B', 16)
    pdf.set_text_color(15, 23, 42)
    pdf.cell(0, 10, '3. Color Palette', 0, 1)
    
    colors = [
        {"name": "Primary (Cool Blue)", "hex": "#3B82F6", "rgb": (59, 130, 246)},
        {"name": "Secondary (Neon Blue)", "hex": "#93C5FD", "rgb": (147, 197, 253)},
        {"name": "Accent (Cyan Glow)", "hex": "#22D3EE", "rgb": (34, 211, 238)},
        {"name": "Background (Ice Blue)", "hex": "#E0F2FE", "rgb": (224, 242, 254)},
        {"name": "Primary Text", "hex": "#0F172A", "rgb": (15, 23, 42)},
        {"name": "Secondary Text", "hex": "#334155", "rgb": (51, 65, 85)}
    ]
    
    for color in colors:
        pdf.set_font('helvetica', 'B', 12)
        pdf.set_text_color(15, 23, 42)
        pdf.cell(60, 10, color['name'], 0, 0)
        pdf.set_font('helvetica', '', 12)
        pdf.cell(40, 10, color['hex'], 0, 0)
        
        # Draw color box
        pdf.set_fill_color(*color['rgb'])
        pdf.rect(pdf.get_x(), pdf.get_y() + 2, 20, 6, 'F')
        pdf.ln(12)
        
    pdf.ln(5)
    
    # 4. Image Examples
    pdf.add_page()
    pdf.set_font('helvetica', 'B', 16)
    pdf.set_text_color(15, 23, 42)
    pdf.cell(0, 10, '4. Brand Imagery & Logos', 0, 1)
    
    pdf.set_font('helvetica', '', 12)
    pdf.set_text_color(51, 65, 85)
    pdf.cell(0, 8, "Below are examples of the brand's logo and background assets from the project:", 0, 1)
    pdf.ln(5)
    
    logo_path = 'src/main/resources/static/images/zentrix_neon_logo.png'
    bg_path = 'src/main/resources/static/images/hero-blue-wave-bg.png'
    
    if os.path.exists(logo_path):
        pdf.cell(0, 10, 'Zentrix Neon Logo:', 0, 1)
        pdf.image(logo_path, x=15, w=100)
        pdf.ln(10)
        
    # Check if a new page is needed for the background
    if pdf.get_y() > 200:
        pdf.add_page()
        
    if os.path.exists(bg_path):
        pdf.cell(0, 10, 'Hero Wave Background Example:', 0, 1)
        pdf.image(bg_path, x=15, w=150)
        pdf.ln(10)

    output_path = 'Youth_Brand_Guidelines.pdf'
    pdf.output(output_path)
    print(f"Successfully created {output_path}")

if __name__ == "__main__":
    create_pdf()
