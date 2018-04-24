INSERT INTO cats VALUES (5, 'Sample', 0);
--
INSERT INTO photos VALUES (1, 1, NULL, '20121114_161713.jpg', 0);
--
INSERT INTO photos VALUES (2, 2, NULL, 'photo.JPG', 0);
--
INSERT INTO photos VALUES (3, 3, NULL, 'winephoto.JPG', 0);
--
INSERT INTO photos VALUES (4, 4, NULL, 'IMG_1897.JPG', 0);
--
INSERT INTO photos VALUES (5, 4, NULL, 'IMG_1896.JPG', 1);
--
INSERT INTO photos VALUES (6, 5, NULL, 'logo.png', 0);
--
INSERT INTO makers VALUES (1, 'Jefferson''s', 'Canada');
--
INSERT INTO makers VALUES (2, 'Russian River Brewing Company', 'Santa Rosa, CA');
--
INSERT INTO makers VALUES (3, 'Four Vines', 'Graton, Sonoma County, California');
--
INSERT INTO makers VALUES (4, '', 'Southern Oromia region, Sidamo');
--
INSERT INTO makers VALUES (5, 'UltraMega', 'California');
--
INSERT INTO flavors VALUES (60, 5, 'Flavor', 0);
--
INSERT INTO flavors VALUES (61, 5, 'Fruit', 1);
--
INSERT INTO flavors VALUES (62, 5, 'Sweet', 2);
--
INSERT INTO flavors VALUES (63, 5, 'Earth', 3);
--
INSERT INTO flavors VALUES (64, 5, 'Bitter', 4);
--
INSERT INTO flavors VALUES (65, 5, 'Linger', 5);
--
INSERT INTO entries_flavors VALUES (1, 1, 'Body', 3, 0);
--
INSERT INTO entries_flavors VALUES (2, 1, 'Charcoal', 4, 1);
--
INSERT INTO entries_flavors VALUES (3, 1, 'Oak', 4, 2);
--
INSERT INTO entries_flavors VALUES (4, 1, 'Leather', 4, 3);
--
INSERT INTO entries_flavors VALUES (5, 1, 'Spice', 2, 4);
--
INSERT INTO entries_flavors VALUES (6, 1, 'Alcohol', 3, 5);
--
INSERT INTO entries_flavors VALUES (7, 1, 'Astringent', 3, 6);
--
INSERT INTO entries_flavors VALUES (8, 1, 'Linger', 4, 7);
--
INSERT INTO entries_flavors VALUES (9, 1, 'Sweet', 2, 8);
--
INSERT INTO entries_flavors VALUES (10, 1, 'Maple', 2, 9);
--
INSERT INTO entries_flavors VALUES (11, 1, 'Fruit', 3, 10);
--
INSERT INTO entries_flavors VALUES (12, 1, 'Vanilla', 2, 11);
--
INSERT INTO entries_flavors VALUES (13, 1, 'Smoke', 1, 12);
--
INSERT INTO entries_flavors VALUES (14, 1, 'Peat', 0, 13);
--
INSERT INTO entries_flavors VALUES (15, 1, 'Nut', 1, 14);
--
INSERT INTO entries_flavors VALUES (16, 2, 'Body', 2, 0);
--
INSERT INTO entries_flavors VALUES (17, 2, 'Syrup', 2, 1);
--
INSERT INTO entries_flavors VALUES (18, 2, 'Fruit', 2, 2);
--
INSERT INTO entries_flavors VALUES (19, 2, 'Citrus', 5, 3);
--
INSERT INTO entries_flavors VALUES (20, 2, 'Hops', 5, 4);
--
INSERT INTO entries_flavors VALUES (21, 2, 'Linger', 4, 5);
--
INSERT INTO entries_flavors VALUES (22, 2, 'Spice', 2, 6);
--
INSERT INTO entries_flavors VALUES (23, 2, 'Herb', 3, 7);
--
INSERT INTO entries_flavors VALUES (24, 2, 'Malt', 2, 8);
--
INSERT INTO entries_flavors VALUES (25, 2, 'Alcohol', 3, 9);
--
INSERT INTO entries_flavors VALUES (26, 2, 'Sweet', 2, 10);
--
INSERT INTO entries_flavors VALUES (27, 2, 'Sour', 3, 11);
--
INSERT INTO entries_flavors VALUES (28, 2, 'Bitter', 3, 12);
--
INSERT INTO entries_flavors VALUES (29, 2, 'Astringent', 2, 13);
--
INSERT INTO entries_flavors VALUES (30, 3, 'Body', 4, 0);
--
INSERT INTO entries_flavors VALUES (31, 3, 'Fruit', 4, 1);
--
INSERT INTO entries_flavors VALUES (32, 3, 'Citrus', 2, 2);
--
INSERT INTO entries_flavors VALUES (33, 3, 'Berry', 4, 3);
--
INSERT INTO entries_flavors VALUES (34, 3, 'Floral', 1, 4);
--
INSERT INTO entries_flavors VALUES (35, 3, 'Spice', 3, 5);
--
INSERT INTO entries_flavors VALUES (36, 3, 'Herb', 2, 6);
--
INSERT INTO entries_flavors VALUES (37, 3, 'Nut', 1, 7);
--
INSERT INTO entries_flavors VALUES (38, 3, 'Earth', 4, 8);
--
INSERT INTO entries_flavors VALUES (39, 3, 'Wood', 3, 9);
--
INSERT INTO entries_flavors VALUES (40, 3, 'Caramel', 2, 10);
--
INSERT INTO entries_flavors VALUES (41, 3, 'Sweet', 3, 11);
--
INSERT INTO entries_flavors VALUES (42, 3, 'Sour', 3, 12);
--
INSERT INTO entries_flavors VALUES (43, 3, 'Astringent', 4, 13);
--
INSERT INTO entries_flavors VALUES (44, 3, 'Linger', 4, 14);
--
INSERT INTO entries_flavors VALUES (45, 3, 'Heat', 4, 15);
--
INSERT INTO entries_flavors VALUES (46, 4, 'Body', 2, 0);
--
INSERT INTO entries_flavors VALUES (47, 4, 'Citrus', 2, 1);
--
INSERT INTO entries_flavors VALUES (48, 4, 'Berry', 5, 2);
--
INSERT INTO entries_flavors VALUES (49, 4, 'Floral', 3, 3);
--
INSERT INTO entries_flavors VALUES (50, 4, 'Spice', 1, 4);
--
INSERT INTO entries_flavors VALUES (51, 4, 'Smoke', 1, 5);
--
INSERT INTO entries_flavors VALUES (52, 4, 'Nut', 3, 6);
--
INSERT INTO entries_flavors VALUES (53, 4, 'Chocolate', 1, 7);
--
INSERT INTO entries_flavors VALUES (54, 4, 'Caramel', 2, 8);
--
INSERT INTO entries_flavors VALUES (55, 4, 'Sweet', 3, 9);
--
INSERT INTO entries_flavors VALUES (56, 4, 'Sour', 2, 10);
--
INSERT INTO entries_flavors VALUES (57, 4, 'Bitter', 1, 11);
--
INSERT INTO entries_flavors VALUES (58, 4, 'Salt', 1, 12);
--
INSERT INTO entries_flavors VALUES (59, 4, 'Finish', 3, 13);
--
INSERT INTO entries_flavors VALUES (60, 5, 'Flavor', 3, 0);
--
INSERT INTO entries_flavors VALUES (61, 5, 'Fruit', 2, 1);
--
INSERT INTO entries_flavors VALUES (62, 5, 'Sweet', 4, 2);
--
INSERT INTO entries_flavors VALUES (63, 5, 'Earth', 5, 3);
--
INSERT INTO entries_flavors VALUES (64, 5, 'Bitter', 3, 4);
--
INSERT INTO entries_flavors VALUES (65, 5, 'Linger', 4, 5);
--
INSERT INTO extras VALUES (23, 5, 'Sample Extra', 0, 0, 0);
--
INSERT INTO extras VALUES (24, 5, 'Extra 2', 1, 0, 0);
--
INSERT INTO entries_extras VALUES (1, 1, 10, 'Straight Rye');
--
INSERT INTO entries_extras VALUES (2, 1, 11, '10 yrs');
--
INSERT INTO entries_extras VALUES (3, 1, 12, '47');
--
INSERT INTO entries_extras VALUES (4, 2, 1, 'Double IPA');
--
INSERT INTO entries_extras VALUES (5, 2, 2, '3');
--
INSERT INTO entries_extras VALUES (6, 2, 3, '100');
--
INSERT INTO entries_extras VALUES (7, 2, 4, '1.07');
--
INSERT INTO entries_extras VALUES (8, 2, 5, '8.0');
--
INSERT INTO entries_extras VALUES (9, 2, 6, '');
--
INSERT INTO entries_extras VALUES (10, 3, 7, 'Zinfandel');
--
INSERT INTO entries_extras VALUES (11, 3, 8, '2009');
--
INSERT INTO entries_extras VALUES (12, 3, 9, '14.4');
--
INSERT INTO entries_extras VALUES (13, 4, 13, 'Herkimer');
--
INSERT INTO entries_extras VALUES (14, 4, 14, '');
--
INSERT INTO entries_extras VALUES (15, 4, 15, '');
--
INSERT INTO entries_extras VALUES (16, 4, 16, '3');
--
INSERT INTO entries_extras VALUES (17, 4, 17, '');
--
INSERT INTO entries_extras VALUES (18, 4, 18, '');
--
INSERT INTO entries_extras VALUES (19, 4, 19, '');
--
INSERT INTO entries_extras VALUES (20, 4, 20, '');
--
INSERT INTO entries_extras VALUES (21, 4, 21, '');
--
INSERT INTO entries_extras VALUES (22, 4, 22, '');
--
INSERT INTO entries_extras VALUES (23, 5, 23, 'A Value');
--
INSERT INTO entries_extras VALUES (24, 5, 24, '8');
--
INSERT INTO entries VALUES (1, 'e030e11e-99c2-4f5e-95c4-6c6740503113', 3, 'Jefferson’s Straight Rye Whiskey', 1, '$36/bottle', 'Home', 1354137274000, 4.0, 'Quick burst of fruit followed by mellow rye spice & dryness, has a grassy linger. There is a certain funk to it that’s VERY subtle, and almost grappa like - very faint and not unpleasant. Makes an above average old fashioned and Manhattan and good neat but not my preferred rye for a session but definitely a dynamic tasting. Chewy linger w/ the grass and leather.');
--
INSERT INTO entries VALUES (2, '3c6c9966-c78a-4a54-93e8-2eb443babdee', 1, 'Pliney The Elder', 2, '$5', 'Bottleworks', 1372219156000, 4.5, 'Profoundly balanced given the alcohol content, hops, and light body. All the characteristics of an imperial but the sum is greater than the parts.');
--
INSERT INTO entries VALUES (3, 'f414553c-c749-458d-b82b-bc18fd9faedd', 2, '2009 Old Vine Cuvee', 3, '$9', '', 1337526402000, 3.5, 'Rich & mineral heavy but with a brightness & heat bursting through to prevent it from lingering metallic, instead leaving behind rich, concentrated cherry and pleasant warmth.');
--
INSERT INTO entries VALUES (4, '2ced63c2-0569-449a-a3c6-608f5f39c5e4', 4, 'Ethiopia Gedeo', 4, '$2', 'Herkimer', 1337526009000, 4.0, 'Strong smell and taste of blueberry, balanced, medium body doesn''t overexpose the dark berry flavor - easy. I like turtles.');
--
INSERT INTO entries VALUES (5, 'bd4a150d-bad0-4b7e-9869-5ae4716babcc', 5, 'Sample Entry', 5, '$6', 'Nowhere', 1440701993000, 3.0, 'This is just a sample...');
