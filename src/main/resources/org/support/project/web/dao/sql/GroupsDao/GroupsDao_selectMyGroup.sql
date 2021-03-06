SELECT
        *
    FROM
        GROUPS
    WHERE
        DELETE_FLAG = 0
        AND 
            EXISTS (
                SELECT
                        *
                    FROM
                        USER_GROUPS
                    WHERE
                        USER_GROUPS.GROUP_ID = GROUPS.GROUP_ID
                        AND USER_GROUPS.USER_ID = ?
                        AND USER_GROUPS.GROUP_ROLE >= 1
            )
    ORDER BY
        GROUP_NAME LIMIT ? OFFSET ?;

