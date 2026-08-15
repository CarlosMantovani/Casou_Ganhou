UPDATE transaction
SET participant_flag_emoji = CASE participant_flag_code
    WHEN 'BRAZIL' THEN '🇧🇷'
    WHEN 'ARGENTINA' THEN '🇦🇷'
    WHEN 'JAPAN' THEN '🇯🇵'
    WHEN 'ITALY' THEN '🇮🇹'
    WHEN 'CANADA' THEN '🇨🇦'
    WHEN 'PORTUGAL' THEN '🇵🇹'
    WHEN 'SPAIN' THEN '🇪🇸'
    WHEN 'FRANCE' THEN '🇫🇷'
    WHEN 'GERMANY' THEN '🇩🇪'
    WHEN 'MEXICO' THEN '🇲🇽'
    WHEN 'URUGUAY' THEN '🇺🇾'
    WHEN 'CHILE' THEN '🇨🇱'
    ELSE participant_flag_emoji
END
WHERE participant_flag_code IS NOT NULL;
