export type ResumeTexTemplateId = 'basic' | 'modern' | 'academic'

export interface ResumeTexTemplate {
  id: ResumeTexTemplateId
  label: string
  description: string
  preamble: (escapedTitle: string) => string
}

const raw = String.raw
const basicHeading = raw`\newcommand{\resumeheading}[2]{%
  \ifcase#1\relax\or\section*{#2}\or\subsection*{#2}\or\subsubsection*{#2}\fi}`

function preamble(margin: number, pageStyle: 'empty' | 'plain', headings: string): ResumeTexTemplate['preamble'] {
  return escapedTitle => raw`\documentclass[UTF8,a4paper,11pt,fontset=fandol]{ctexart}
\usepackage[margin=${margin}mm]{geometry}
\usepackage{amsmath}
\usepackage{amssymb}
\usepackage{booktabs}
\usepackage{enumitem}
\usepackage{longtable}
\usepackage[hidelinks]{hyperref}
\pagestyle{${pageStyle}}
\setlength{\parindent}{0pt}
\setlength{\parskip}{0.35em}
${headings}
\newcommand{\resumebody}[1]{\par #1\par}
\newlist{resumeitemize}{itemize}{1}
\setlist[resumeitemize]{label=\textbullet}
\newlist{resumeenumerate}{enumerate}{1}
\setlist[resumeenumerate]{label=\arabic*.}
\newenvironment{resumetable}[1]{%
  \ifdim\dimexpr\linewidth/#1\relax<4\tabcolsep
    \setlength{\tabcolsep}{\dimexpr\linewidth/#1/4\relax}%
  \fi
  \begin{longtable}{*{#1}{p{\dimexpr\linewidth/#1-2\tabcolsep\relax}}}}
  {\end{longtable}}
\newcommand{\resumerule}{\par\noindent\rule{\linewidth}{0.4pt}\par}
\newcommand{\resumeasset}[1]{\resumebody{#1}}
\hypersetup{pdftitle={${escapedTitle}}}`
}

const contact = raw`\newcommand{\resumecontact}[1]{\par{\centering\small #1\par}}`
const modernHeadings = raw`\newcommand{\resumename}[1]{\par{\centering\LARGE\sffamily\bfseries #1\par}\nobreak}
${contact}
\newcommand{\resumeheading}[2]{%
  \ifcase#1\relax
  \or\section*{#2}%
  \or\par\addvspace{0.8em}{\raggedright\large\sffamily\bfseries #2\par}\nobreak
     \noindent\rule{\linewidth}{0.4pt}\par\nobreak
  \or\par\addvspace{0.45em}{\raggedright\normalsize\bfseries #2\par}\nobreak
  \fi}`

const academicHeadings = raw`\newcommand{\resumename}[1]{\par{\centering\LARGE\bfseries #1\par}\nobreak}
${contact}
\ctexset{subsection={beforeskip=1.6em,afterskip=0.6em},subsubsection={beforeskip=1em,afterskip=0.35em}}
${basicHeading}`

export const resumeTexTemplates: Readonly<Record<ResumeTexTemplateId, ResumeTexTemplate>> = {
  basic: {
    id: 'basic',
    label: '基础',
    description: '基础单栏，16 mm 页边距，无页码。',
    preamble: preamble(16, 'empty', basicHeading),
  },
  modern: {
    id: 'modern',
    label: '现代简洁',
    description: '居中姓名、细线分区，16 mm 页边距，无页码。',
    preamble: preamble(16, 'empty', modernHeadings),
  },
  academic: {
    id: 'academic',
    label: '学术简历',
    description: '分区留白较大，20 mm 页边距，页码居中。',
    preamble: preamble(20, 'plain', academicHeadings),
  },
}
