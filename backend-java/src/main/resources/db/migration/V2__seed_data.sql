-- Development seed data (DO NOT use in production)

INSERT INTO users (name, email, password_hash, role) VALUES
    ('Admin MDB', 'admin@mdb.dev', '$2b$10$TZk6plALpzN9vwRwyvDQXuXEgL0sDt3np3rsSpfuG1zGfDxwPapK6', 'ADMIN'),
    ('Demo User', 'demo@mdb.dev', '$2b$10$Wn41evPM20YsnNOw3ntP6O8nEIH8GknUmK7xh9jaHCXsgb2xS7Jg6', 'USER');
-- Development passwords: admin@mdb.dev / admin123  |  demo@mdb.dev / demo123

INSERT INTO areas (name) VALUES
    ('Algebra'), ('Geometry'), ('Combinatorics'), ('Number Theory'),
    ('Calculus'), ('Probability'), ('Logic');

INSERT INTO tags (name) VALUES
    ('equations'), ('factoring'), ('polynomials'), ('triangles'), ('circles'),
    ('counting'), ('permutations'), ('divisibility'), ('primes'), ('limits'),
    ('derivatives'), ('conditional probability'), ('propositions'), ('induction'), ('sequences');

-- Example problems (created_by = admin, id 1)
INSERT INTO problems (title, statement, hint, solution, source, initial_difficulty, area_id, created_by) VALUES
('Simple quadratic equation',
 'Solve the equation $x^2 - 5x + 6 = 0$.',
 'Try factoring the polynomial as a product of two binomials.',
 'Factoring: $(x-2)(x-3)=0$, so $x = 2$ or $x = 3$.',
 'Algebra problem set', 2,
 (SELECT id FROM areas WHERE name = 'Algebra'), 1),

('Sum of roots',
 'If $a$ and $b$ are the roots of $x^2 - 7x + 10 = 0$, find $a+b$ and $a \cdot b$.',
 'Use Vieta''s formulas for a quadratic equation.',
 'By Vieta''s formulas, $a+b = 7$ and $a \cdot b = 10$.',
 NULL, 2,
 (SELECT id FROM areas WHERE name = 'Algebra'), 1),

('Difference-of-squares factoring',
 'Fully factor the expression $x^4 - 16$.',
 'Remember that $a^2 - b^2 = (a-b)(a+b)$ and apply it twice.',
 '$x^4-16=(x^2-4)(x^2+4)=(x-2)(x+2)(x^2+4)$.',
 NULL, 3,
 (SELECT id FROM areas WHERE name = 'Algebra'), 1),

('2x2 linear system',
 'Solve the system $\begin{cases} x + y = 10 \\ x - y = 2 \end{cases}$.',
 'Add the two equations to eliminate $y$.',
 'Adding: $2x = 12 \Rightarrow x=6$; substituting, $y=4$.',
 NULL, 1,
 (SELECT id FROM areas WHERE name = 'Algebra'), 1),

('Triangle area by coordinates',
 'Find the area of the triangle with vertices $(0,0)$, $(4,0)$ and $(0,3)$.',
 'Since two sides lie on the axes, use base times height over 2.',
 'Base $=4$, height $=3$, area $= \frac{4\cdot 3}{2} = 6$.',
 NULL, 1,
 (SELECT id FROM areas WHERE name = 'Geometry'), 1),

('Applying the Pythagorean theorem',
 'A right triangle has legs $6$ and $8$. Find the hypotenuse.',
 'Apply the Pythagorean theorem $a^2+b^2=c^2$ directly.',
 '$c = \sqrt{6^2+8^2} = \sqrt{100} = 10$.',
 NULL, 1,
 (SELECT id FROM areas WHERE name = 'Geometry'), 1),

('Interior angles of a polygon',
 'What is the sum of the interior angles of a convex octagon?',
 'Use the formula $(n-2)\cdot 180°$ for convex polygons with $n$ sides.',
 '$(8-2)\cdot 180° = 1080°$.',
 NULL, 2,
 (SELECT id FROM areas WHERE name = 'Geometry'), 1),

('Circumference of a circle',
 'A circle has radius $5$ cm. Find its circumference in terms of $\pi$.',
 'The circumference of a circle is $C = 2\pi r$.',
 '$C = 2\pi \cdot 5 = 10\pi$ cm.',
 NULL, 1,
 (SELECT id FROM areas WHERE name = 'Geometry'), 1),

('Counting anagrams',
 'How many distinct anagrams does the word "MATH" have?',
 'All letters are distinct; use simple permutations.',
 'Since all 4 letters are distinct, the total is $4! = 24$ anagrams.',
 NULL, 2,
 (SELECT id FROM areas WHERE name = 'Combinatorics'), 1),

('Simple combinations',
 'In how many ways can a committee of 3 people be chosen from 8 candidates?',
 'The order of selection does not matter; use combinations.',
 '$C(8,3) = \frac{8!}{3!5!} = 56$.',
 NULL, 3,
 (SELECT id FROM areas WHERE name = 'Combinatorics'), 1),

('Multiplicative principle',
 'A menu has 3 starters, 4 main courses and 2 desserts. How many distinct full meals are possible?',
 'Multiply the number of options at each independent step.',
 'By the multiplicative principle: $3 \times 4 \times 2 = 24$ meals.',
 NULL, 1,
 (SELECT id FROM areas WHERE name = 'Combinatorics'), 1),

('Divisibility by 9',
 'Determine whether $47238$ is divisible by 9.',
 'Add the digits and check whether the sum is divisible by 9.',
 'The digit sum is $4+7+2+3+8=24$, which is not divisible by 9; so $47238$ is not divisible by 9.',
 NULL, 2,
 (SELECT id FROM areas WHERE name = 'Number Theory'), 1),

('Prime numbers up to 20',
 'List all prime numbers less than 20.',
 'Remember that a prime number has exactly two distinct divisors.',
 'The primes less than 20 are: $2, 3, 5, 7, 11, 13, 17, 19$.',
 NULL, 1,
 (SELECT id FROM areas WHERE name = 'Number Theory'), 1),

('Greatest common divisor',
 'Find the GCD of 48 and 18.',
 'Use the Euclidean algorithm, dividing successively.',
 '$48 = 2\cdot18+12$; $18=1\cdot12+6$; $12=2\cdot6+0$. So $\gcd(48,18)=6$.',
 NULL, 2,
 (SELECT id FROM areas WHERE name = 'Number Theory'), 1),

('Limit of a rational function',
 'Compute $\lim_{x \to 2} \frac{x^2-4}{x-2}$.',
 'Factor the numerator to cancel the $0/0$ indeterminate form.',
 '$\frac{x^2-4}{x-2}=\frac{(x-2)(x+2)}{x-2}=x+2$, so the limit is $2+2=4$.',
 NULL, 3,
 (SELECT id FROM areas WHERE name = 'Calculus'), 1),

('Derivative of a polynomial function',
 'Find the derivative of $f(x) = 3x^3 - 2x^2 + 5x - 7$.',
 'Apply the power rule term by term.',
 '$f''(x) = 9x^2 - 4x + 5$.',
 NULL, 2,
 (SELECT id FROM areas WHERE name = 'Calculus'), 1),

('Probability with a fair die',
 'Rolling a fair 6-sided die once, what is the probability of getting an even number?',
 'Count the favorable outcomes among the possible ones.',
 'The even outcomes are $\{2,4,6\}$, so $P = \frac{3}{6} = \frac{1}{2}$.',
 NULL, 1,
 (SELECT id FROM areas WHERE name = 'Probability'), 1),

('Simple conditional probability',
 'An urn has 5 red balls and 3 blue balls. Drawing two balls without replacement, what is the probability both are red?',
 'Compute the probability of the first being red and, given that, the second also being red.',
 '$P = \frac{5}{8}\cdot\frac{4}{7} = \frac{20}{56} = \frac{5}{14}$.',
 NULL, 4,
 (SELECT id FROM areas WHERE name = 'Probability'), 1),

('Truth table of an implication',
 'Determine the truth value of $p \to q$ when $p$ is false and $q$ is true.',
 'Remember that an implication is only false when the antecedent is true and the consequent is false.',
 'Since $p$ is false, the implication $p \to q$ is true, regardless of $q$.',
 NULL, 1,
 (SELECT id FROM areas WHERE name = 'Logic'), 1),

('Negation of compound propositions',
 'Write the negation of "Every even integer is divisible by 4".',
 'Negating "for all" gives "there exists some" that does not satisfy the condition.',
 'The negation is: "There exists an even integer that is not divisible by 4".',
 NULL, 3,
 (SELECT id FROM areas WHERE name = 'Logic'), 1);

-- Attach some tags to problems
INSERT INTO problem_tags (problem_id, tag_id)
SELECT p.id, t.id FROM problems p, tags t
WHERE (p.title = 'Simple quadratic equation' AND t.name IN ('equations','factoring'))
   OR (p.title = 'Sum of roots' AND t.name IN ('equations','polynomials'))
   OR (p.title = 'Difference-of-squares factoring' AND t.name IN ('factoring','polynomials'))
   OR (p.title = '2x2 linear system' AND t.name IN ('equations'))
   OR (p.title = 'Triangle area by coordinates' AND t.name IN ('triangles'))
   OR (p.title = 'Applying the Pythagorean theorem' AND t.name IN ('triangles'))
   OR (p.title = 'Circumference of a circle' AND t.name IN ('circles'))
   OR (p.title = 'Counting anagrams' AND t.name IN ('counting','permutations'))
   OR (p.title = 'Simple combinations' AND t.name IN ('counting'))
   OR (p.title = 'Multiplicative principle' AND t.name IN ('counting'))
   OR (p.title = 'Divisibility by 9' AND t.name IN ('divisibility'))
   OR (p.title = 'Prime numbers up to 20' AND t.name IN ('primes'))
   OR (p.title = 'Greatest common divisor' AND t.name IN ('divisibility'))
   OR (p.title = 'Limit of a rational function' AND t.name IN ('limits'))
   OR (p.title = 'Derivative of a polynomial function' AND t.name IN ('derivatives'))
   OR (p.title = 'Simple conditional probability' AND t.name IN ('conditional probability'))
   OR (p.title = 'Negation of compound propositions' AND t.name IN ('propositions'));

-- Some difficulty ratings from the demo user (id 2)
INSERT INTO difficulty_ratings (user_id, problem_id, difficulty)
SELECT 2, id, initial_difficulty FROM problems WHERE title IN
    ('Simple quadratic equation', 'Simple combinations', 'Simple conditional probability');

INSERT INTO user_problem_status (user_id, problem_id, status)
SELECT 2, id, 'FAVORITE' FROM problems WHERE title = 'Applying the Pythagorean theorem';
INSERT INTO user_problem_status (user_id, problem_id, status)
SELECT 2, id, 'WANT_TO_DO' FROM problems WHERE title = 'Negation of compound propositions';
