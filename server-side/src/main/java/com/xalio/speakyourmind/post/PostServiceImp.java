package com.xalio.speakyourmind.post;


import com.xalio.speakyourmind.comment.Comment;
import com.xalio.speakyourmind.comment.CommentDTO;
import com.xalio.speakyourmind.comment.CommentMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostServiceImp {

	private final PostRepository postRepository;
	private final PostMapper postMapper;
	private final CommentMapper commentMapper;
	private final HttpServletRequest request;


	public List<PostDTO> getAllPosts() {

		return postRepository.findAll(Sort.by("upVote")
		                                  .descending())
		                     .stream()
		                     .map(post -> {
			                     PostDTO postDTO = postMapper.postToPostDto(post);
			                     postDTO.setHasVoted(hasUserReacted(post));
			                     return postDTO;
		                     })
		                     .collect(Collectors.toList());
	}

	public PostDTO getPostById(UUID id) throws NotFoundException {
		Post post = postRepository.findById(id)
		                          .orElseThrow(NotFoundException::new);
		if (hasUserReacted(post)) {
			post.setHasVoted(true);
		}
		return postMapper.postToPostDto(post);
	}


	public void newPost(PostDTO postDTO) {
		postDTO.setUpVote(1);
		postRepository.save(postMapper.postDtoToPost(postDTO));

	}

	public void newComment(UUID id, CommentDTO commentDTO) throws NotFoundException {
		Post post = postRepository.findById(id)
		                          .orElseThrow(NotFoundException::new);
		post.getCommentList()
		    .add(commentMapper.commentDtoToComment(commentDTO));
		postRepository.save(post);
	}


	public void patchPostUpVote(UUID id) throws NotFoundException {
		Post post = postRepository.findById(id)
		                          .orElseThrow(NotFoundException::new);
		String userIpAddress = request.getRemoteAddr();
		String encodedUserIpAddress = Base64.getEncoder()
		                                    .encodeToString(userIpAddress.getBytes());
		if (!post.getUpVotedIPs()
		         .contains(encodedUserIpAddress)) {
			post.setUpVote(post.getUpVote() + 1);
			post.getUpVotedIPs()
			    .add(encodedUserIpAddress);
			postRepository.save(post);
		}

	}

	public List<CommentDTO> getAllCommentsByPostId(UUID id) {
		return postRepository.findById(id)
		                     .orElseThrow(() -> new RuntimeException("Post not found with id: " + id))
		                     .getCommentList()
		                     .stream()
		                     .sorted(Comparator.comparing(Comment::getCreatedAt)
		                                       .reversed())
		                     .map(commentMapper::commentToCommentDto)
		                     .collect(Collectors.toList());
	}

	// Check if the user reacted or not and edit hasVoted depends on that
	private boolean hasUserReacted(Post post) {
		if (post != null) {
			String userIpAddress = request.getRemoteAddr();
			String encodedUserIpAddress = Base64.getEncoder()
			                                    .encodeToString(userIpAddress.getBytes());
			return post.getUpVotedIPs()
			           .contains(encodedUserIpAddress);
		}
		return false;
	}
}
