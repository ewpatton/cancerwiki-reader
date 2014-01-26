select page.page_id, page.page_namespace, page.page_title, revision.rev_timestamp, text.old_text from
  page join revision on page.page_id = revision.rev_page
join text on revision.rev_text_id = text.old_id
join (select page.page_id, max(revision.rev_id) as latest_edit from
  page join revision on page.page_id = revision.rev_page where page.page_namespace = 0 group by page.page_id) as latest
on latest.page_id = page.page_id and latest.latest_edit = revision.rev_id
where text.old_text like "%{{Recommendation%" or text.old_text like "%{{Experience%";